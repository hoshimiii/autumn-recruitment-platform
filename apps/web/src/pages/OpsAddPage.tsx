import { useEffect, useState } from "react";
import { ApiError, createManualItem, listManualItems } from "../api/client";
import type { ManualActionInput, ManualItem } from "../api/client";
import { PageHeader } from "../features/layout/PageHeader";
import { ManualActionForm } from "../features/manual/ManualActionForm";
import { ManualRecordList } from "../features/manual/ManualRecordList";

export function OpsAddPage() {
  const [records, setRecords] = useState<ManualItem[]>([]); const [error, setError] = useState<string>();
  useEffect(() => { const controller = new AbortController(); listManualItems(controller.signal).then(setRecords).catch((reason: unknown) => { if (!controller.signal.aborted) setError(reason instanceof ApiError ? reason.message : "无法加载人工记录。"); }); return () => controller.abort(); }, []);
  const save = async (record: ManualActionInput) => { try { const created = await createManualItem(record); setRecords((items) => [created, ...items]); } catch (reason) { setError(reason instanceof ApiError ? reason.message : "保存人工记录失败。"); throw reason; } };
  return <main className="workbench"><PageHeader title="添加/导入" description="保存登录、验证码或人工核验边界内的记录，不触发自动采集。" />{error ? <section className="panel state-panel" role="alert">{error}</section> : null}<div className="ops-grid"><section className="panel" aria-labelledby="import-contract-title"><div className="section-heading"><h2 id="import-contract-title">导入规范</h2><span>结构化插槽</span></div><ul className="stack-list"><li><div><strong>岗位主信息</strong><span>类型、公司、岗位、城市、开始和截止日期。</span></div></li><li><div><strong>筛选信息</strong><span>技术栈、证书/资质、优先级、来源可信度。</span></div></li><li><div><strong>阻塞信息</strong><span>登录、验证码、人工复核、投递方式和来源链接。</span></div></li></ul></section><div className="secondary-column"><ManualActionForm onSubmit={save} /><ManualRecordList records={records} /></div></div></main>;
}
