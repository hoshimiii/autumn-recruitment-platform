import assert from 'node:assert/strict';
import { createServer } from 'node:http';
import { createServer as createPortProbe } from 'node:net';
import { spawn, spawnSync } from 'node:child_process';
import { createHash, randomUUID } from 'node:crypto';
import { createWriteStream, mkdirSync } from 'node:fs';
import { resolve } from 'node:path';

// This smoke requires the isolated demo Redis/ES containers; it stops them to test fallback.
const api='http://127.0.0.1:18189/api/v1', datasource=`jdbc:sqlite:file:middleware-${randomUUID()}?mode=memory&cache=shared`;
mkdirSync('output/runtime',{recursive:true});
const log=createWriteStream('output/runtime/middleware-server.log');
const javaEnv=Object.fromEntries(Object.entries(process.env).filter(([key])=>
  !/^(SPRING_|RECRUITOPS_|SERVER_|LOGGING_|JAVA_TOOL_OPTIONS$|JDK_JAVA_OPTIONS$|_JAVA_OPTIONS$)/i.test(key)));
let backend, sourceServer, sourceId, namespace, javaPid, launcherIdentity, jvmIdentity, result;
let started=false, processError=false, startupBuffer='';
function command(executable,args,options={}) {
  const response=spawnSync(executable,args,{encoding:'utf8',windowsHide:true,timeout:60_000,...options});
  assert.equal(response.status,0,'Owned runtime command failed (output withheld)');
  return response.stdout.trim();
}
const compose=(...args)=>{
  return command('docker',['compose','-p','recruitops-middleware-demo','-f','infra/compose.yml',...args],{
    env:{...process.env,RECRUITOPS_KEYCLOAK_ADMIN_PASSWORD:'unused-for-cache-search-test'}});
};
function processIdentity(pid) {
  if(process.platform!=='win32')return {ProcessId:pid,CreationDate:pid};
  assert.ok(Number.isSafeInteger(pid)&&pid>0);
  const output=command('powershell.exe',['-NoProfile','-NonInteractive','-Command',
    `Get-CimInstance Win32_Process -Filter 'ProcessId=${pid}' | Select-Object ProcessId,ParentProcessId,Name,CreationDate | ConvertTo-Json -Compress`]);
  return output?JSON.parse(output):null;
}
function sameProcess(identity) {
  const current=identity&&processIdentity(identity.ProcessId);
  return current&&current.CreationDate===identity.CreationDate;
}
function captureJvm() {
  if(!javaPid||!backend)return;
  const candidate=processIdentity(javaPid);
  assert.ok(candidate&&(process.platform!=='win32'||(/^java(?:w)?\.exe$/i.test(candidate.Name)
    &&(candidate.ProcessId===backend.pid||candidate.ParentProcessId===backend.pid))),'Cannot verify owned JVM');
  jvmIdentity=candidate;
}
function cleanupJava() {
  if(!jvmIdentity&&javaPid)captureJvm();
  for(const identity of new Map([jvmIdentity,launcherIdentity].filter(Boolean).map(item=>[item.ProcessId,item])).values()) {
    if(!sameProcess(identity))continue;
    if(process.platform==='win32')command('taskkill',['/pid',String(identity.ProcessId),'/T','/F']);
    else process.kill(identity.ProcessId,'SIGTERM');
    if(process.platform==='win32')assert.ok(!sameProcess(identity),'Owned Java process survived cleanup');
  }
}
for(const [signal,code] of [['SIGINT',130],['SIGTERM',143]])process.once(signal,()=>{
  try {cleanupJava();} catch {console.error(JSON.stringify({status:'FAIL',stage:'interrupted owned Java cleanup'}));}
  sourceServer?.close();log.end();process.exit(code);
});
const sleep=ms=>new Promise(resolve=>setTimeout(resolve,ms));
async function until(check,message) {
  const end=Date.now()+40_000;
  while(Date.now()<end) {if(await Promise.resolve().then(check).catch(()=>false))return;await sleep(300);}
  throw new Error(message);
}
const query=filters=>apiRequest(api,`/jobs?${new URLSearchParams(filters)}`);
async function apiRequest(base,path,body) {
  const response=await fetch(`${base}${path}`,{method:body===undefined?'GET':'POST',redirect:'error',signal:AbortSignal.timeout(15_000),
    headers:{Accept:'application/json',...(body===undefined?{}:{'Content-Type':'application/json'})},
    ...(body===undefined?{}:{body:JSON.stringify(body)})});
  assert.ok(response.ok,`Synthetic API ${path} failed (${response.status})`);
  return response.json();
}
let row={companyName:'Synthetic Runtime Company',companyCategory:'INTERNET',title:'Backend Engineer',
  businessUnit:'Platform',recruitmentYear:2027,officialJobId:'synthetic-role-1',sourceRowId:'row-1',
  itemDiscriminator:'role-1',jobType:'FULL_TIME',city:'Shanghai',deadline:'2099-12-31',jd:'Original Java API description',sourceTrust:'OFFICIAL'};
try {
  const probe=createPortProbe();
  await new Promise((done,reject)=>{probe.once('error',reject);probe.listen(18189,'127.0.0.1',done);});
  await new Promise(done=>probe.close(done));
  for(const service of ['redis','elasticsearch']) {
    const containers=compose('ps','--status','running','-q',service).split(/\r?\n/).filter(Boolean);
    assert.equal(containers.length,1);
    const labels=JSON.parse(command('docker',['inspect','--format','{{json .Config.Labels}}',containers[0]]));
    assert.equal(labels['com.docker.compose.project'],'recruitops-middleware-demo');
    assert.equal(labels['com.docker.compose.service'],service);
    assert.ok(labels['com.docker.compose.project.config_files'].split(',').some(file=>
      file.replaceAll('\\','/').toLowerCase()===resolve('infra/compose.yml').replaceAll('\\','/').toLowerCase()),'Unexpected Compose owner');
    const ports=JSON.parse(command('docker',['inspect','--format','{{json .NetworkSettings.Ports}}',containers[0]]));
    const expected=service==='redis'?['6379/tcp','16379']:['9200/tcp','19200'];
    assert.deepEqual(ports[expected[0]],[{HostIp:'127.0.0.1',HostPort:expected[1]}],'Unexpected middleware endpoint');
  }
  const health=await fetch('http://127.0.0.1:19200/_cluster/health',{signal:AbortSignal.timeout(10_000)}).then(r=>r.json());
  assert.ok(['yellow','green'].includes(health.status));assert.equal(compose('exec','-T','redis','redis-cli','ping'),'PONG');
  sourceServer=createServer((req,res)=>{
    if(req.url==='/robots.txt'){res.end('User-agent: *\nAllow: /\n');return;}
    if(req.url==='/jobs'){res.setHeader('Content-Type','application/json');res.end(JSON.stringify({items:[row]}));return;}
    res.end('Synthetic local runtime fixture');
  });
  await new Promise(resolve=>sourceServer.listen(0,'127.0.0.1',resolve));
  const origin=`http://127.0.0.1:${sourceServer.address().port}`;row.sourceUrl=origin+'/jobs/role-1';
  backend=spawn('java',['-Xmx384m','-jar',resolve('apps/server/target/recruitment-server-0.1.0-SNAPSHOT.jar'),
    '--server.port=18189','--server.address=127.0.0.1','--recruitops.security.mode=local',`--spring.datasource.url=${datasource}`,'--recruitops.collector.allow-private-addresses=true',
    '--recruitops.collector.minimum-floor-seconds=0','--recruitops.collector.scheduler-enabled=false',
    '--recruitops.redis.enabled=true','--spring.data.redis.host=127.0.0.1','--spring.data.redis.port=16379','--spring.data.redis.timeout=1s',
    '--spring.data.redis.connect-timeout=1s','--recruitops.elasticsearch.enabled=true','--recruitops.elasticsearch.url=http://127.0.0.1:19200','--recruitops.elasticsearch.sync-delay-ms=200'],
    {stdio:['ignore','pipe','pipe'],windowsHide:true,env:javaEnv});
  backend.stdout.pipe(log,{end:false});backend.stderr.pipe(log,{end:false});
  backend.stdout.on('data',chunk=>{startupBuffer=(startupBuffer+String(chunk)).slice(-16_384);const match=startupBuffer.match(/with PID (\d+)/);if(match)javaPid=Number(match[1]);if(startupBuffer.includes('Started RecruitOpsApplication'))started=true;});
  backend.on('error',()=>{processError=true;});
  launcherIdentity=processIdentity(backend.pid);
  await until(()=>!processError&&started&&apiRequest(api,'/health').then(()=>true),'Owned isolated Java backend did not start');
  assert.ok(javaPid,'Actual JVM PID was reported by the owned child');
  captureJvm();
  namespace=createHash('sha256').update(`${datasource}:${javaPid}`).digest('hex').slice(0,24);
  const source=await apiRequest(api,'/sources',{name:'Synthetic runtime source',type:'COMPANY',baseUrl:origin,listUrl:origin+'/jobs',
    adapterType:'JSON',mode:'JSON',complianceStatus:'ALLOWED',trustLevel:'OFFICIAL',companyCategory:'INTERNET',enabled:true,
    minIntervalSeconds:0,maxPages:1,maxItems:2,timeoutSeconds:5,selectors:'{"itemsPath":"items"}',fieldMapping:'{}',
    termsUrl:origin+'/terms',termsConfirmedAt:new Date().toISOString(),robotsUrl:origin+'/robots.txt'});
  sourceId=source.id;
  async function publish() {
    const run=await apiRequest(api,`/sources/${sourceId}/runs`,{});assert.equal(run.reviewItemCount,1);
    const pending=(await apiRequest(api,'/review-items')).items.filter(item=>String(item.sourceId)===String(sourceId));
    assert.equal(pending.length,1);return apiRequest(api,`/review-items/${pending[0].id}/approve`,{});
  }
  const published=await publish();
  await until(()=>apiRequest(api,'/projections').then(status=>status.enabled&&!status.pending&&!status.lastError),'ES projection did not refresh/ack');
  const filters={company:row.companyName,businessUnit:row.businessUnit,title:row.title,recruitmentYear:'2027',keyword:'Backend Enginer'};
  const fuzzy=await query(filters);assert.equal(fuzzy.total,1);assert.equal(fuzzy.items[0].id,published.id);
  const index=`recruitops-jobs-${namespace}`;
  const firstDocument=await fetch(`http://127.0.0.1:19200/${index}/_doc/${published.id}`,{signal:AbortSignal.timeout(10_000)}).then(r=>r.json());
  assert.equal(firstDocument._source.jd,row.jd);
  const keys=compose('exec','-T','redis','redis-cli','--scan','--pattern',`recruitops:jobs:${namespace}:*`).split('\n').filter(Boolean);
  assert.ok(keys.length>0,'Actual Redis contains generation-scoped query cache');
  const hits=()=>Number(compose('exec','-T','redis','redis-cli','INFO','stats').match(/keyspace_hits:(\d+)/)[1]);
  const beforeHits=hits();assert.equal((await query(filters)).total,1);assert.ok(hits()>beforeHits,'Second API read hits actual Redis');
  const exactFilters={company:row.companyName};
  assert.equal((await query(exactFilters)).items[0].jd,row.jd);
  const exactHits=hits();assert.equal((await query(exactFilters)).items[0].jd,row.jd);assert.ok(hits()>exactHits,'Cache original exact query before update');
  row={...row,jd:'Changed Java API description'};
  const updated=await publish();assert.equal(updated.id,published.id);
  assert.equal((await query(exactFilters)).items[0].jd,row.jd,'Committed SQL update invalidates the same cached query');
  await until(()=>apiRequest(api,'/projections').then(status=>!status.pending),'Changed ES projection did not refresh');
  assert.equal((await query(filters)).items[0].jd,row.jd,'Updated fuzzy query also replaces its old cached result');
  const changedDocument=await fetch(`http://127.0.0.1:19200/${index}/_doc/${published.id}`,{signal:AbortSignal.timeout(10_000)}).then(r=>r.json());
  assert.equal(changedDocument._source.jd,row.jd);assert.ok(changedDocument._version>firstDocument._version);
  compose('stop','redis');
  assert.equal((await query({company:row.companyName,title:row.title,businessUnit:'Platform'})).total,1,'Redis outage preserves SQL reads');
  compose('stop','elasticsearch');
  assert.equal((await query({company:row.companyName,keyword:'Backend'})).total,1,'ES outage falls back to literal SQL');
  assert.equal((await query({company:row.companyName,keyword:'Backend Engineeer'})).total,0,'Unavailable fuzzy engine does not invent matches');
  result={status:'PASS',redis:'actual cache hit, same-query invalidation and outage fallback',elasticsearch:'actual fuzzy query, refresh, versioned update and outage fallback',published:1,namespace};
} finally {
  try {cleanupJava();} finally {await new Promise(resolve=>sourceServer?sourceServer.close(resolve):resolve());log.end();}
}
console.log(JSON.stringify(result));
