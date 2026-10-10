import { spawn } from 'node:child_process';
import { fileURLToPath } from 'node:url';
import { requireLoopbackApi } from './protocol.mjs';
const args=process.argv.slice(2);const option=name=>{const index=args.indexOf(name);return index<0?undefined:args[index+1];};
const api=option('--api'),ids=(option('--tasks')??'').split(','),parallel=Number(option('--workers')??2);
requireLoopbackApi(api);
if(![1,2].includes(parallel)||ids.length>100||ids.some(id=>!/^\d+$/.test(id))) throw new Error('Provide fixed task IDs and at most two workers');
const children=new Set();let next=0,failed=false;
async function run(slot) {
  while(next<ids.length && !failed) {
    const id=ids[next++];
    const code=await new Promise(resolve=>{const child=spawn(process.execPath,[fileURLToPath(new URL('./worker.mjs',import.meta.url)),api,id,`browser-worker-${process.pid}-${slot}`,args.includes('--headed')?'headed':'headless'],{stdio:'inherit',windowsHide:true});children.add(child);child.on('error',()=>resolve(1));child.on('exit',code=>{children.delete(child);resolve(code??1);});});
    if(code!==0) failed=true;
  }
}
process.on('SIGINT',()=>{for(const child of children) child.kill();process.exitCode=1;});
await Promise.all(Array.from({length:Math.min(parallel,ids.length)},(_,index)=>run(index)));
if(failed) process.exitCode=1;
