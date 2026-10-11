import { useEffect, useState } from "react";
import { ApiError, completeBrowserTask, createBrowserTask, listBrowserTasks, listSources } from "../../api/client";
import type { BrowserCompletion, BrowserTask, Source } from "../../api/client";

export function BrowserPendingImport() {
  const [sources,setSources]=useState<Source[]>([]),[tasks,setTasks]=useState<BrowserTask[]>([]);
  const [error,setError]=useState<string>(),[message,setMessage]=useState<string>(),[saving,setSaving]=useState(false);
  useEffect(()=>{const controller=new AbortController();Promise.all([listSources(controller.signal),listBrowserTasks(controller.signal)]).then(([items,records])=>{setSources(items);setTasks(records);}).catch(reason=>{if(!controller.signal.aborted)setError(reason instanceof ApiError?reason.message:"无法加载浏览器任务。");});return()=>controller.abort();},[]);
  const create=async(event:React.FormEvent<HTMLFormElement>)=>{
    event.preventDefault();const data=new FormData(event.currentTarget);setError(undefined);setMessage(undefined);setSaving(true);
    try {const task=await createBrowserTask({sourceId:Number(data.get("source")),rowStart:Number(data.get("start")),rowEnd:Number(data.get("end")),maxChars:20_000});setTasks(items=>[task,...items]);setMessage(`任务 ${task.id} 已创建，采集完成后进入人工复核。`);}
    catch(reason){setError(reason instanceof ApiError?reason.message:"创建浏览器任务失败。");}finally{setSaving(false);}
  };
  const complete=async(event:React.FormEvent<HTMLFormElement>)=>{
    event.preventDefault();setError(undefined);setMessage(undefined);
    let envelope: BrowserCompletion & {taskId:number};
    try {envelope=JSON.parse(String(new FormData(event.currentTarget).get("result"))) as typeof envelope;if(!Number.isSafeInteger(envelope.taskId)||envelope.taskId<1||typeof envelope.workerId!=="string"||!envelope.workerId||typeof envelope.leaseToken!=="string"||!envelope.leaseToken||!Array.isArray(envelope.rows))throw new Error();}
    catch{setError("请提供完整的任务、工作进程、租约和行证据 JSON。");return;}
    setSaving(true);
    try {const task=await completeBrowserTask(envelope.taskId,{workerId:envelope.workerId,leaseToken:envelope.leaseToken,rows:envelope.rows});setTasks(items=>[task,...items.filter(item=>item.id!==task.id)]);setMessage("已导入待审核结果。公司线索保存在人工记录，独立岗位进入复核队列。");}
    catch(reason){setError(reason instanceof ApiError?reason.message:"导入失败，请核对租约与来源证据。");}finally{setSaving(false);}
  };
  return <section className="panel" aria-labelledby="browser-import-title"><div className="section-heading"><h2 id="browser-import-title">浏览器待审核导入</h2><span>来源批准 · 行证据 · 人工复核</span></div>
    <p>仅接受已单独批准浏览器采集的公开来源。部门与毕业届别未知时保留空值，遇到登录、验证码或复制限制即停止。</p>
    <form className="manual-form" onSubmit={create}><label>已配置来源<select name="source" required defaultValue=""><option value="" disabled>选择来源</option>{sources.filter(source=>source.enabled&&source.complianceStatus==="ALLOWED").map(source=><option key={source.id} value={source.id}>{source.name}</option>)}</select></label><div className="form-grid"><label>起始行（从 0 开始）<input name="start" type="number" min="0" defaultValue="0" required /></label><label>结束行（不含）<input name="end" type="number" min="1" defaultValue="1" required /></label></div><button disabled={saving}>创建浏览器任务</button></form>
    <form className="manual-form" onSubmit={complete}><label>工作进程结果 JSON<textarea name="result" aria-label="工作进程结果 JSON" rows={7} required maxLength={100_000} placeholder="粘贴包含 taskId、workerId、leaseToken 和 rows 的结果" /></label><button disabled={saving}>导入待审核结果</button></form>
    {error?<p role="alert" className="form-error">{error}</p>:null}{message?<p role="status">{message}</p>:null}
    {tasks.length?<ul className="stack-list">{tasks.slice(0,5).map(task=><li key={task.id}><strong>任务 {task.id}</strong><span>行 {task.rowStart}–{task.rowEnd} · {task.status}</span></li>)}</ul>:null}
  </section>;
}
