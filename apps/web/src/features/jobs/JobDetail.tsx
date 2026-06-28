import type { Job } from "../../api/client";

type JobDetailProps = {
  job: Job;
};

export function JobDetail({ job }: JobDetailProps) {
  const manualStatus = job.manualActionStatus === "done" ? "无需人工" : job.manualActionStatus;

  return (
    <aside className="panel detail-panel" aria-labelledby="detail-title">
      <div className="section-heading">
        <h2 id="detail-title">岗位详情</h2>
        <span>{job.status}</span>
      </div>
      <div className="detail-header">
        <strong>{job.title}</strong>
        <span>{job.companyName} · {job.city}</span>
      </div>
      <section className="metadata-grid" aria-label="来源与人工状态">
        <span>来源 {job.sourceName} · {job.sourceType}</span>
        <span>来源链接 {job.sourceUrl}</span>
        <span>置信度 {Math.round(job.sourceConfidence * 100)}%</span>
        <span>人工状态 {manualStatus}</span>
        <span>登录 {job.sourceRequiresLogin ? "是" : "否"}</span>
        <span>验证码 {job.sourceRequiresCaptcha ? "是" : "否"}</span>
        {job.requiredAction ? <span>需处理动作 {job.requiredAction}</span> : null}
      </section>
      <section>
        <h3>JD</h3>
        <p>{job.description}</p>
      </section>
      <section>
        <h3>要求</h3>
        <ul>
          {job.requirements.map((item) => (
            <li key={item}>{item}</li>
          ))}
        </ul>
      </section>
      <section className="tag-section">
        <h3>岗位技术栈</h3>
        <div className="tag-row">
          {job.techStack.map((item) => (
            <span className="tag" key={item}>{item}</span>
          ))}
        </div>
      </section>
      <section className="tag-section">
        <h3>候选证书与资质</h3>
        <div className="tag-row">
          {job.certificateRequirements.map((item) => (
            <span className="tag" key={item}>{item}</span>
          ))}
        </div>
      </section>
      <section>
        <h3>来源快照</h3>
        <ul>
          {job.snapshots.map((item) => (
            <li key={item}>{item}</li>
          ))}
        </ul>
      </section>
      <section>
        <h3>投递信息</h3>
        <p>{job.applicationInfo}</p>
      </section>
    </aside>
  );
}
