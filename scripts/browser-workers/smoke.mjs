import { spawn } from 'node:child_process';
import { fileURLToPath } from 'node:url';
import assert from 'node:assert/strict';
import { startFixture } from './fixture.mjs';
import { apiRequest,requireLoopbackApi } from './protocol.mjs';
const api=process.argv[2]??'http://127.0.0.1:8080/api/v1';requireLoopbackApi(api);
const providerStub=process.argv.includes('--provider-stub');
const fixture=await startFixture();
try {
  const source=await apiRequest(api,'/sources',{name:'Browser worker acceptance fixture',type:'COMPANY',baseUrl:fixture.origin,listUrl:fixture.origin+'/table',adapterType:'HTML',mode:'MANUAL',complianceStatus:'ALLOWED',trustLevel:'OFFICIAL',companyCategory:'INTERNET',enabled:true,minIntervalSeconds:1,maxPages:1,maxItems:2,timeoutSeconds:5,selectors:JSON.stringify({row:'article'}),termsUrl:fixture.origin+'/terms',termsConfirmedAt:new Date().toISOString(),robotsUrl:fixture.origin+'/robots.txt'});
  await apiRequest(api,`/browser-tasks/sources/${source.id}/approve`,{visibleBrowserApproved:true,adapter:providerStub?'sub2api':'fixture'});
  const tasks=await Promise.all([0,1].map(rowStart=>apiRequest(api,'/browser-tasks',{sourceId:source.id,rowStart,rowEnd:rowStart+1,maxChars:4000})));
  const code=await new Promise(resolve=>{const child=spawn(process.execPath,[fileURLToPath(new URL('./supervisor.mjs',import.meta.url)),'--api',api,'--tasks',tasks.map(t=>t.id).join(','),'--workers','2'],{stdio:'inherit',windowsHide:true,env:{...process.env,...(providerStub?{SUB2API_BASE_URL:fixture.origin,SUB2API_TOKEN:'fixture-only',SUB2API_MODEL:'fixture-model'}:{})}});child.on('error',()=>resolve(1));child.on('exit',code=>resolve(code));});
  assert.equal(code,0,'Two independent browser workers complete');
  const reviews=await apiRequest(api,'/review-items'),manuals=await apiRequest(api,'/manual-items');
  assert.equal(reviews.items.filter(item=>String(item.sourceId)===String(source.id)).length,1);
  assert.ok(manuals.items.some(item=>item.companyName==='Fixture Company' && item.notes.includes(`"taskId":${tasks[0].id}`)));
  const completed=await apiRequest(api,'/browser-tasks');
  for(const task of tasks) assert.equal(completed.items.find(item=>item.id===task.id).status,'COMPLETED');
  const reads=fixture.requests.filter(req=>req.url==='/table');assert.equal(reads.length,2);
  assert.ok(Math.abs(reads[0].at-reads[1].at)>=850,'Shared source pacing across processes');
  assert.ok(reads.every(req=>req.userAgent?.startsWith('AutumnRecruitmentBot/')));
  if(providerStub) assert.equal(fixture.requests.filter(req=>req.url==='/v1/chat/completions').length,2);
  console.log(JSON.stringify({status:'PASS',adapter:providerStub?'local-provider-stub':'fixture',sourceId:source.id,tasks:tasks.map(task=>task.id),manual:1,pendingReview:1,published:0,sourceReads:reads.length}));
} finally {await fixture.close();}
