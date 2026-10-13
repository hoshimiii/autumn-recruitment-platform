import test from 'node:test';
import assert from 'node:assert/strict';
import { chromium } from '@playwright/test';
import { startFixture } from './fixture.mjs';
import { installRequestPolicy } from './request-policy.mjs';
import { AccessBarrierError } from './protocol.mjs';

async function exercise(path,{revokeWhileWaiting=false}={}) {
  const fixture=await startFixture();let browser;
  try {browser=await chromium.launch({channel:'chrome'});}catch(error){await fixture.close();throw error;}
  const context=await browser.newContext({serviceWorkers:'block'});
  const closed=new Promise(resolve=>context.once('close',resolve));
  const checks=[],stops=[],reservations=[];let revoked=false;
  let stopped;const stoppedPromise=new Promise(resolve=>{stopped=resolve;});
  const validate=async url=>{checks.push(url);if(revoked)throw new Error('Source policy revoked');if(new URL(url).pathname.startsWith('/denied'))throw new AccessBarrierError('robots_denied');};
  await installRequestPolicy(context,{origin:fixture.origin,leaseExpiresAt:Date.now()+120_000,
    check:validate,validate,
    pace:async url=>{reservations.push(url);if(revokeWhileWaiting){setTimeout(()=>{revoked=true;},20);return Date.now()+100;}return Date.now();},
    stop:reason=>{stops.push(reason);stopped(reason);},fail:reason=>stopped(reason)});
  try {
    const page=await context.newPage();await page.goto(fixture.origin+path,{waitUntil:'domcontentloaded',timeout:5000}).catch(()=>{});
    const reason=await Promise.race([stoppedPromise,new Promise((_,reject)=>{const timer=setTimeout(()=>reject(new Error('Expected request barrier stop')),5000);timer.unref();})]);
    await closed;
    await assert.rejects(()=>page.title());
    return {requests:fixture.requests,checks,reservations,stops,reason};
  } finally {await browser.close();await fixture.close();}
}
test('denied redirect target is checked but never fetched',async()=>{
  const result=await exercise('/redirect-denied');
  assert.ok(result.checks.some(url=>url.endsWith('/denied-target')));
  assert.equal(result.requests.filter(req=>req.url==='/denied-target').length,0);
  assert.match(result.reason,/robots_denied/);
});
test('denied same-origin XHR stops before dispatch',async()=>{
  const result=await exercise('/xhr-denied');
  assert.ok(result.checks.some(url=>url.endsWith('/denied-xhr')));
  assert.equal(result.requests.filter(req=>req.url==='/denied-xhr').length,0);
});
test('403 response closes context before a follow-up request can leave',async()=>{
  const result=await exercise('/barrier-followup');
  assert.equal(result.requests.filter(req=>req.url.startsWith('/blocked-api')).length,1);
  assert.equal(result.requests.filter(req=>req.url==='/after-barrier').length,0);
  assert.match(result.reason,/403/);assert.equal(result.stops.length,1);
});
for(const status of [401,429])test(`${status} barrier blocks all follow-up requests`,async()=>{
  const result=await exercise(`/barrier-followup?status=${status}`);
  assert.equal(result.requests.filter(req=>req.url.startsWith('/blocked-api')).length,1);
  assert.equal(result.requests.filter(req=>req.url==='/after-barrier').length,0);
  assert.match(result.reason,new RegExp(String(status)));assert.equal(result.stops.length,1);
});
test('peer policy revoked during pacing prevents the first navigation',async()=>{
  const result=await exercise('/table',{revokeWhileWaiting:true});
  assert.equal(result.reservations.length,1);assert.equal(result.checks.length,2);
  assert.equal(result.requests.filter(req=>req.url==='/table').length,0);assert.match(result.reason,/revoked/);
});
test('approved-origin redirect is rejected before dispatching its target',async()=>{
  for(const [path,target] of [['/redirect-allowed','/table'],['/redirect-nested','/jobs/table']]) {
    const result=await exercise(path);
    assert.equal(result.requests.filter(req=>req.url===target).length,0);
    assert.match(result.reason,/redirect.*final URL/i);
  }
});
test('configured final URL preserves relative scripts and location semantics',async()=>{
  const fixture=await startFixture();let browser;
  try {
    browser=await chromium.launch({channel:'chrome'});
    const context=await browser.newContext({serviceWorkers:'block'});const stops=[];
    const policy=await installRequestPolicy(context,{origin:fixture.origin,leaseExpiresAt:Date.now()+120_000,
      check:async()=>{},validate:async()=>{},pace:async()=>Date.now(),stop:async reason=>stops.push(reason)});
    const page=await context.newPage();await page.goto(fixture.origin+'/jobs/table');
    await page.getByText('/jobs/table',{exact:true}).waitFor();
    assert.equal(page.url(),fixture.origin+'/jobs/table');
    assert.ok(fixture.requests.some(req=>req.url==='/jobs/relative.js'));
    assert.ok(!fixture.requests.some(req=>req.url==='/relative.js'));
    assert.deepEqual(stops,[]);await policy.finish();
  } finally {await browser?.close();await fixture.close();}
});
test('every approved XHR reserves pacing and revalidates before fetching',async()=>{
  const fixture=await startFixture();let browser;
  try {
    browser=await chromium.launch({channel:'chrome'});
    for(const path of ['/xhr-allowed']) {
      const context=await browser.newContext({serviceWorkers:'block'});const checks=[],paced=[],stops=[];let finalUrl;
      const policy=await installRequestPolicy(context,{origin:fixture.origin,leaseExpiresAt:Date.now()+120_000,
        check:async url=>checks.push(url),validate:async url=>checks.push(url),pace:async url=>{paced.push(url);return Date.now();},stop:async reason=>stops.push(reason),onDocumentUrl:url=>{finalUrl=url;}});
      const page=await context.newPage();await page.goto(fixture.origin+path);
      if(path==='/xhr-allowed')await page.getByText('done',{exact:true}).waitFor();
      await policy.finish();
      assert.equal(stops.length,0);assert.equal(paced.length,2);assert.equal(checks.length,4);
      for(const url of paced)assert.equal(checks.filter(item=>item===url).length,2);
      assert.equal(finalUrl,fixture.origin+path);
    }
  } finally {await browser?.close();await fixture.close();}
});
test('ordinary request-check failure closes this context without revoking source approval',async()=>{
  const fixture=await startFixture();let browser;
  try {
    browser=await chromium.launch({channel:'chrome'});const context=await browser.newContext();
    const failures=[],stops=[];const closed=new Promise(resolve=>context.once('close',resolve));
    await installRequestPolicy(context,{origin:fixture.origin,leaseExpiresAt:Date.now()+120_000,
      check:async()=>{throw new Error('Worker API unavailable');},validate:async()=>{},pace:async()=>Date.now(),
      stop:async reason=>stops.push(reason),fail:async reason=>failures.push(reason)});
    const page=await context.newPage();await page.goto(fixture.origin+'/table').catch(()=>{});await closed;
    assert.equal(stops.length,0);assert.equal(failures.length,1);
    assert.match(failures[0],/unavailable/);assert.equal(fixture.requests.length,0);
  } finally {await browser?.close();await fixture.close();}
});

test('final robots recheck is paced before document and XHR requests leave',async()=>{
  const fixture=await startFixture();let browser;
  try {
    browser=await chromium.launch({channel:'chrome'});
    const context=await browser.newContext({serviceWorkers:'block'});const failures=[];
    let nextReadAt=0;
    const interval=120;
    const reserve=()=>{const at=Math.max(Date.now(),nextReadAt);nextReadAt=at+interval;return at;};
    const policy=await installRequestPolicy(context,{origin:fixture.origin,leaseExpiresAt:Date.now()+120_000,
      check:async()=>{await new Promise(resolve=>setTimeout(resolve,Math.max(0,reserve()-Date.now())));
        const response=await fetch(fixture.origin+'/robots.txt');assert.equal(response.status,200);await response.text();},
      pace:async()=>reserve(),validate:async()=>{},stop:async reason=>failures.push(reason),fail:async reason=>failures.push(reason)});
    const page=await context.newPage();await page.goto(fixture.origin+'/xhr-allowed');
    await page.getByText('done',{exact:true}).waitFor();await policy.finish();
    assert.deepEqual(failures,[]);
    for(const path of ['/xhr-allowed','/allowed-api']) {
      const index=fixture.requests.findIndex(read=>read.url===path);
      assert.ok(index>0);assert.equal(fixture.requests[index-1].url,'/robots.txt');
      assert.ok(fixture.requests[index].at-fixture.requests[index-1].at>=interval-20,
        `${path} immediately followed robots: ${JSON.stringify(fixture.requests)}`);
    }
  } finally {await browser?.close();await fixture.close();}
});
