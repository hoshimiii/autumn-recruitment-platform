import test from 'node:test';
import assert from 'node:assert/strict';
import { createServer } from 'node:http';
import { spawn } from 'node:child_process';
import { startFixture } from './fixture.mjs';

async function runWorker({selectors='{"row":"article"}',path='/table',adapter='fixture',checkBarrier=false}={}) {
  const fixture=await startFixture();const calls=[];
  const server=createServer(async(req,res)=>{
    let text='';for await(const chunk of req)text+=chunk;
    calls.push({path:req.url,body:text?JSON.parse(text):null});res.setHeader('Content-Type','application/json');
    if(req.url==='/browser-tasks/1/claim')res.end(JSON.stringify({id:1,workerId:'test-worker',leaseToken:'fixture-lease',fixtureMode:true,
      origin:fixture.origin,listUrl:fixture.origin+path,leaseExpiresAt:Date.now()+120_000,rowStart:0,rowEnd:1,maxRows:1,maxChars:4000,selectors,adapter}));
    else if(req.url==='/browser-tasks/1/check-request'&&checkBarrier){res.writeHead(403);res.end('{"error":{"code":"browser_access_barrier","message":"robots_denied"}}');}
    else if(req.url==='/browser-tasks/1/pace')res.end(JSON.stringify({readAt:Date.now()}));
    else if(req.url==='/v1/chat/completions'){res.writeHead(503);res.end('{"error":"local provider unavailable"}');}
    else res.end('{"status":"ALLOWED"}');
  });
  await new Promise(resolve=>server.listen(0,'127.0.0.1',resolve));
  const api=`http://127.0.0.1:${server.address().port}`;
  try {
    const child=spawn(process.execPath,['scripts/browser-workers/worker.mjs',api,'1','test-worker'],
      {env:{...process.env,BROWSER_WORKER_TOKEN:'',SUB2API_BASE_URL:api,SUB2API_TOKEN:'fixture-only',SUB2API_MODEL:'fixture-model'},stdio:['ignore','pipe','pipe']});
    let stderr='';child.stderr.on('data',data=>{stderr+=data;});child.stdout.resume();
    const timer=setTimeout(()=>child.kill(),20_000);
    const code=await new Promise((resolve,reject)=>{child.once('error',reject);child.once('exit',resolve);});clearTimeout(timer);
    assert.equal(code,1,stderr);return calls;
  } finally {await new Promise(resolve=>server.close(resolve));await fixture.close();}
}

for(const [name,options] of [['malformed selector',{selectors:'{'}],['local provider outage',{adapter:'sub2api'}],['source HTTP 500',{path:'/server-error'}]]) {
  test(`${name} fails only its claimed task`,async()=>{
    const calls=await runWorker(options);
    assert.equal(calls.filter(call=>call.path.endsWith('/fail')).length,1);
    assert.equal(calls.filter(call=>call.path.endsWith('/stop')).length,0);
    assert.equal(calls.filter(call=>call.path.endsWith('/complete')).length,0);
  });
}
for(const [name,options] of [['visible login',{path:'/auth-required'}],['visible captcha',{path:'/verify'}],['copy restriction',{path:'/copy-denied'}],['export restriction',{path:'/export-denied'}],
  ['unapproved redirect',{path:'/redirect-unapproved'}],...[401,403,429].map(status=>[`HTTP ${status}`,{path:`/blocked-api?status=${status}`}]),['robots denial',{checkBarrier:true}]]) {
  test(`${name} stops and revokes the source`,async()=>{
    const calls=await runWorker(options);
    assert.equal(calls.filter(call=>call.path.endsWith('/stop')).length,1);
    assert.equal(calls.filter(call=>call.path.endsWith('/fail')).length,0);
    assert.equal(calls.filter(call=>call.path.endsWith('/complete')).length,0);
  });
}
