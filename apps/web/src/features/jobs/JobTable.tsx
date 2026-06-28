import type { Job } from "../../api/client";

type JobTableProps = {
  jobs: Job[];
  selectedJobId: string;
  onSelectJob: (jobId: string) => void;
};

export function JobTable({ jobs, selectedJobId, onSelectJob }: JobTableProps) {
  return (
    <section className="panel job-table-panel" aria-labelledby="jobs-title">
      <div className="section-heading">
        <h2 id="jobs-title">岗位列表</h2>
        <span>{jobs.length} 条岗位</span>
      </div>
      <div className="table-wrap">
        <table>
          <thead>
            <tr>
              <th>类型</th>
              <th>公司</th>
              <th>岗位</th>
              <th>岗位类型</th>
              <th>城市</th>
              <th>技术栈</th>
              <th>证书/资质</th>
              <th>截止日期</th>
              <th>状态</th>
            </tr>
          </thead>
          <tbody>
            {jobs.map((job) => (
              <tr
                key={job.id}
                className={job.id === selectedJobId ? "is-selected" : undefined}
              >
                <td>{job.sourceType}</td>
                <td>{job.company}</td>
                <td>
                  <button type="button" className="text-button" onClick={() => onSelectJob(job.id)}>
                    {job.title}
                  </button>
                </td>
                <td>{job.jobType}</td>
                <td>{job.city}</td>
                <td>{job.techStack.join(", ")}</td>
                <td>{job.certificates.join(", ")}</td>
                <td>{job.deadline}</td>
                <td>
                  <span className={`status status-${job.status}`}>{job.status}</span>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </section>
  );
}
