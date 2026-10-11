import type { ReviewDetail } from "../../api/client";
import { displayLabel } from "../../api/labels";

export function ReviewEvidence({ item }: { item?: ReviewDetail }) {
  return <section className="panel evidence-panel" aria-labelledby="evidence-title">
    <div className="section-heading"><h2 id="evidence-title">候选证据</h2>
      <span>{item ? displayLabel(item.status) : "选择一条记录"}</span></div>
    {!item ? <p className="empty-state">选择待复核岗位以核对来源和字段。</p> : <>
      <div className="metadata-grid">
        <span>部门 {String(item.normalizedPayload.businessUnit ?? "未知")}</span>
        <span>毕业届别 {String(item.normalizedPayload.recruitmentYear ?? "未知")}</span>
        <span>来源行 {String(item.normalizedPayload.sourceRowId ?? "未知")}</span>
        <span>来源 {item.sourceName ?? "-"}</span>
        {item.sourceUrl ? <a href={item.sourceUrl} target="_blank" rel="noreferrer">原始链接</a> : <span>无原始链接</span>}
        <span>内容哈希 <span title={item.contentHash}>{item.contentHash?.slice(0, 12) ?? "-"}</span></span>
        <span>重复岗位 {item.duplicateJobId ?? "未命中"}</span>
      </div>
      <section><h3>原始候选</h3><pre>{JSON.stringify(item.candidatePayload, null, 2)}</pre></section>
      <section><h3>标准化字段</h3><pre>{JSON.stringify(item.normalizedPayload, null, 2)}</pre></section>
    </>}
  </section>;
}
