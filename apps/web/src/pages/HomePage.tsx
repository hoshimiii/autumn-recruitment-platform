import type { Job, JobFilters as JobFilterState } from "../api/client";
import { JobDetail } from "../features/jobs/JobDetail";
import { JobFilters } from "../features/jobs/JobFilters";
import { JobTable } from "../features/jobs/JobTable";
import { PageHeader } from "../features/layout/PageHeader";
import type { AppRoute } from "../routing/routes";

type HomePageProps = {
  filters: JobFilterState;
  filteredJobs: Job[];
  hrefFor: (path: AppRoute) => string;
  jobs: Job[];
  onFiltersChange: (filters: JobFilterState) => void;
  onNavigate: (path: AppRoute) => void;
  onSelectJob: (jobId: string) => void;
  selectedJob?: Job;
  selectedJobId: string;
};

export function HomePage({
  filters,
  filteredJobs,
  hrefFor,
  jobs,
  onFiltersChange,
  onNavigate,
  onSelectJob,
  selectedJob,
  selectedJobId
}: HomePageProps) {
  const summary = [
    { label: "岗位", value: jobs.length },
    { label: "开放", value: jobs.filter((job) => job.deadlineStatus === "开放").length },
    { label: "临近截止", value: jobs.filter((job) => job.deadlineStatus === "临近").length },
    { label: "P1", value: jobs.filter((job) => job.priority === 1).length }
  ];

  return (
    <main className="workbench">
      <PageHeader
        title="岗位展示"
        description="集中查看秋招岗位、技术栈、证书要求与投递关键信息。"
        actions={(
          <>
            <a className="button-link secondary-button" href={hrefFor("/ops/review")} onClick={(event) => {
              event.preventDefault();
              onNavigate("/ops/review");
            }}>进入复核</a>
            <a className="button-link" href={hrefFor("/ops/add")} onClick={(event) => {
              event.preventDefault();
              onNavigate("/ops/add");
            }}>添加岗位</a>
          </>
        )}
      />

      <section className="summary-band" aria-label="岗位概览">
        {summary.map((item) => (
          <div className="summary-item" key={item.label}>
            <span>{item.label}</span>
            <strong>{item.value}</strong>
          </div>
        ))}
      </section>

      <JobFilters jobs={jobs} filters={filters} onChange={onFiltersChange} />

      <div className="primary-grid">
        <JobTable jobs={filteredJobs} selectedJobId={selectedJobId} onSelectJob={onSelectJob} />
        {selectedJob ? <JobDetail job={selectedJob} /> : null}
      </div>
    </main>
  );
}
