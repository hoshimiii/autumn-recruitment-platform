import { createServer } from 'node:http';
import { extractFixture } from './protocol.mjs';
const rows=[
  'RowId: visible-row-001\nItem: company\nKind: COMPANY\nCompany: Fixture Company\nDepartment: Platform\nCohort: 2027',
  'RowId: visible-row-002\nItem: official-role-007\nKind: ROLE\nCompany: Fixture Company\nTitle: Java Engineer\nDepartment: Cloud\nCohort: 2027\nCity: Shanghai\nOfficialJobId: official-role-007\nJD: Build Java services\nJobType: FULL_TIME\nRequirements: Java'
];
export async function startFixture() {
  const requests=[];
  const server=createServer(async(req,res)=>{
    requests.push({url:req.url,userAgent:req.headers['user-agent'],at:Date.now()});
    if(req.url==='/robots.txt'){res.setHeader('Content-Type','text/plain');res.end('User-agent: *\nAllow: /\nDisallow: /denied\n');return;}
    if(req.url==='/terms'){res.end('Local non-billable fixture: visible extraction is approved.');return;}
    if(req.url==='/v1/chat/completions'){
      let body='';for await(const chunk of req){body+=chunk;if(body.length>100_000){res.writeHead(413);res.end();return;}}
      const input=JSON.parse(body);const value=extractFixture(input.messages.at(-1).content);
      res.setHeader('Content-Type','application/json');res.end(JSON.stringify({choices:[{message:{content:JSON.stringify(value)}}]}));return;
    }
    if(req.url==='/table') {res.setHeader('Content-Type','text/html');res.end(`<!doctype html><html><head><title>Approved visible recruitment fixture</title></head><body><h1>Public recruiting table</h1>${rows.map(row=>`<article><pre>${row}</pre></article>`).join('')}</body></html>`);return;}
    if(req.url==='/redirect-denied'){res.writeHead(302,{Location:'/denied-target'});res.end();return;}
    if(req.url==='/redirect-allowed'){res.writeHead(302,{Location:'/table'});res.end();return;}
    if(req.url==='/redirect-nested'){res.writeHead(302,{Location:'/jobs/table'});res.end();return;}
    if(req.url==='/jobs/table'){res.setHeader('Content-Type','text/html');res.end('<body><script src="./relative.js"></script></body>');return;}
    if(req.url==='/jobs/relative.js'){res.setHeader('Content-Type','application/javascript');res.end('document.body.textContent=location.pathname');return;}
    if(req.url==='/xhr-denied'){res.setHeader('Content-Type','text/html');res.end('<script>fetch("/denied-xhr").catch(()=>{})</script>');return;}
    if(req.url==='/xhr-allowed'){res.setHeader('Content-Type','text/html');res.end('<script>fetch("/allowed-api").then(()=>document.body.textContent="done")</script><body></body>');return;}
    if(req.url==='/allowed-api'){res.end('allowed');return;}
    if(req.url?.startsWith('/barrier-followup')){const requested=Number(new URL(req.url,'http://fixture').searchParams.get('status'));const status=[401,403,429].includes(requested)?requested:403;res.setHeader('Content-Type','text/html');res.end(`<script>fetch("/blocked-api?status=${status}").then(()=>fetch("/after-barrier")).catch(()=>{})</script>`);return;}
    if(req.url?.startsWith('/blocked-api')){const status=Number(new URL(req.url,'http://fixture').searchParams.get('status'));res.writeHead([401,403,429].includes(status)?status:403);res.end('Access barrier');return;}
    if(req.url?.startsWith('/denied') || req.url==='/after-barrier'){res.end('This endpoint must not be fetched');return;}
    if(req.url==='/captcha'){res.writeHead(200);res.end('<h1>captcha 验证码</h1>');return;}
    if(req.url==='/server-error'){res.writeHead(500);res.end('Temporary server error');return;}
    if(req.url==='/auth-required'){res.setHeader('Content-Type','text/html');res.end('<h1>login required</h1>');return;}
    if(req.url==='/verify'){res.setHeader('Content-Type','text/html');res.end('<h1>captcha 验证码</h1>');return;}
    if(req.url==='/export-denied'){res.setHeader('Content-Type','text/html');res.end('<body data-export-denied="true">Visible source</body>');return;}
    if(req.url==='/redirect-unapproved'){res.writeHead(302,{Location:'https://unapproved.example/jobs'});res.end();return;}
    if(req.url==='/copy-denied'){res.setHeader('Content-Type','text/html');res.end('<body data-copy-denied="true">Visible source</body>');return;}
    res.writeHead(404);res.end();
  });
  await new Promise(resolve=>server.listen(0,'127.0.0.1',resolve));
  return {origin:`http://127.0.0.1:${server.address().port}`,requests,close:()=>new Promise(resolve=>server.close(resolve))};
}
