import { chromium } from '@playwright/test';
import { lookup } from 'node:dns/promises';
import { isIP } from 'node:net';
import { apiRequest, extractFixture, extractWithProvider, requireOrigin, requireLoopbackApi, AccessBarrierError } from './protocol.mjs';
import { installRequestPolicy } from './request-policy.mjs';

const [api,taskId,workerId,headed]=process.argv.slice(2);
requireLoopbackApi(api);
if(!/^\d+$/.test(taskId??'') || !workerId) throw new Error('Fixed task ID and worker ID required');
const base=`/browser-tasks/${taskId}`;
let browser,lease,policy,context,documentUrl,failureReported=false;
const report=async(path,reason)=>{if(!lease || failureReported)return;failureReported=true;await apiRequest(api,`${base}/${path}`,{workerId,leaseToken:lease.leaseToken,reason:reason.slice(0,300)}).catch(()=>{});};
const stop=reason=>report('stop',reason),fail=reason=>report('fail',reason);
const deadline=setTimeout(()=>{if(policy)void policy.stop(new Error('Worker time budget exceeded'));else{void context?.close();void browser?.close();void fail('Worker time budget exceeded');}},105_000);
try {
  lease=await apiRequest(api,`${base}/claim`,{workerId});
  requireOrigin(lease.listUrl,lease.origin);
  if(!lease.fixtureMode) {
    const addresses=await lookup(new URL(lease.origin).hostname,{all:true});
    if(addresses.some(({address})=>privateAddress(address))) throw new AccessBarrierError('Private source address rejected');
  } else if(!['127.0.0.1','localhost','[::1]'].includes(new URL(lease.origin).hostname)) throw new AccessBarrierError('Fixture origin must be loopback');
  browser=await chromium.launch({channel:'chrome',headless:headed!=='headed'});
  context=await browser.newContext({userAgent:'AutumnRecruitmentBot/1.0 (visible browser worker)',serviceWorkers:'block'});
  policy=await installRequestPolicy(context,{origin:lease.origin,leaseExpiresAt:lease.leaseExpiresAt,stop,fail,
    check:url=>apiRequest(api,`${base}/check-request`,{workerId,leaseToken:lease.leaseToken,requestUrl:url}),
    validate:()=>apiRequest(api,`${base}/check-lease`,{workerId,leaseToken:lease.leaseToken}),
    pace:async()=>{const result=await apiRequest(api,`${base}/pace`,{workerId,leaseToken:lease.leaseToken});return result.readAt;},
    onDocumentUrl:url=>{documentUrl=url;}});
  const page=await context.newPage();
  const response=await page.goto(lease.listUrl,{waitUntil:'domcontentloaded',timeout:Math.max(1,Math.min(90_000,lease.leaseExpiresAt-Date.now()-20_000))});
  if(!response?.ok()) throw policy.error??new Error(`Source response ${response?.status()}`);
  requireOrigin(page.url(),lease.origin);
  const text=await page.locator('body').innerText({timeout:10_000});
  if(text.length>50_000) throw new Error('Visible page budget exceeded');
  if(/captcha|验证码|登录验证|login required|禁止复制|禁止导出|copy(?:ing)? (?:is )?(?:denied|disabled|prohibited)|export (?:is )?(?:denied|disabled|prohibited)/i.test(text)) throw new AccessBarrierError('Visible login/captcha/copy/export restriction');
  if(await page.locator('body[data-copy-denied="true"],body[data-export-denied="true"],input[type="password"]:visible').count()) throw new AccessBarrierError('Visible access/copy restriction');
  if(policy.error) throw policy.error;
  const selectors=JSON.parse(lease.selectors);if(typeof selectors.row!=='string' || selectors.row.length>300) throw new Error('Approved row selector missing');
  const locator=page.locator(selectors.row);await locator.nth(lease.rowEnd-1).waitFor({state:'visible',timeout:10_000});const count=await locator.count();
  if(count<lease.rowEnd) throw new Error('Configured visible row range not present');
  const rows=[];let chars=0;
  for(let index=lease.rowStart;index<lease.rowEnd;index++) {
    const item=locator.nth(index);if(!await item.isVisible()) throw new Error('Hidden row rejected');
    const copyingDenied=await item.evaluate(node=>getComputedStyle(node).userSelect==='none' || !node.dispatchEvent(new Event('copy',{bubbles:true,cancelable:true})));
    if(copyingDenied) throw new AccessBarrierError('Source copy restriction');
    const visibleText=await item.innerText();chars+=visibleText.length;if(chars>lease.maxChars) throw new Error('Visible text budget exceeded');
    const extracted=lease.adapter==='fixture'?extractFixture(visibleText):await extractWithProvider(visibleText,{baseUrl:process.env.SUB2API_BASE_URL??'',token:process.env.SUB2API_TOKEN??'',model:process.env.SUB2API_MODEL??''});
    rows.push({...extracted,rowIndex:index,sourceUrl:documentUrl??page.url(),capturedAt:new Date().toISOString(),visibleText});
  }
  if(policy.error) throw policy.error;
  await policy.finish();if(policy.error)throw policy.error;
  const result=await apiRequest(api,`${base}/complete`,{workerId,leaseToken:lease.leaseToken,rows});
  process.stdout.write(JSON.stringify({taskId:Number(taskId),workerId,status:result.status,rows:rows.length})+'\n');
} catch(error) {
  const failure=policy?.error??error;
  if(policy)await policy.stop(failure);else{await context?.close().catch(()=>{});await (failure instanceof AccessBarrierError?stop:fail)(failure.message);}
  process.stderr.write(failure.message+'\n');process.exitCode=1;
} finally {clearTimeout(deadline);await browser?.close();}
function privateAddress(ip) {
  if(isIP(ip)===4) {const [a,b]=ip.split('.').map(Number);return a===0||a===10||a===127||a===169&&b===254||a===172&&b>=16&&b<=31||a===192&&b===168||a>=224;}
  return ip==='::1'||ip==='::'||/^(fc|fd|fe[89ab])/i.test(ip)||/^::ffff:/i.test(ip);
}
