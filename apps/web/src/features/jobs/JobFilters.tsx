import type { Job, JobFilters as JobFiltersState } from "../../api/client";
import { uniqueValues } from "../../api/client";

type JobFiltersProps = {
  jobs: Job[];
  filters: JobFiltersState;
  onChange: (filters: JobFiltersState) => void;
};

export function JobFilters({ jobs, filters, onChange }: JobFiltersProps) {
  const update = (key: keyof JobFiltersState, value: string) => {
    onChange({ ...filters, [key]: value });
  };

  const jobTypes = uniqueValues(jobs.map((job) => job.jobType));
  const sourceTypes = uniqueValues(jobs.map((job) => job.sourceType));
  const statuses = uniqueValues(jobs.map((job) => job.status));
  const techStacks = uniqueValues(jobs.flatMap((job) => job.techStack));
  const certificates = uniqueValues(jobs.flatMap((job) => job.certificateRequirements));
  const manualStates = uniqueValues(jobs.map((job) => job.manualActionStatus));

  return (
    <section className="panel filter-panel" aria-label="岗位筛选">
      <label>
        关键词
        <input
          value={filters.keyword}
          onChange={(event) => update("keyword", event.target.value)}
          placeholder="公司、岗位、城市"
        />
      </label>
      <label>
        岗位类型
        <select value={filters.jobType} onChange={(event) => update("jobType", event.target.value)}>
          <option value="">全部</option>
          {jobTypes.map((value) => <option key={value}>{value}</option>)}
        </select>
      </label>
      <label>
        来源类型
        <select value={filters.sourceType} onChange={(event) => update("sourceType", event.target.value)}>
          <option value="">全部</option>
          {sourceTypes.map((value) => <option key={value}>{value}</option>)}
        </select>
      </label>
      <label>
        状态
        <select value={filters.status} onChange={(event) => update("status", event.target.value)}>
          <option value="">全部</option>
          {statuses.map((value) => <option key={value}>{value}</option>)}
        </select>
      </label>
      <label>
        技术栈筛选
        <select
          aria-label="技术栈筛选"
          value={filters.techStack}
          onChange={(event) => update("techStack", event.target.value)}
        >
          <option value="">全部</option>
          {techStacks.map((value) => <option key={value}>{value}</option>)}
        </select>
      </label>
      <label>
        证书筛选
        <select value={filters.certificate} onChange={(event) => update("certificate", event.target.value)}>
          <option value="">全部</option>
          {certificates.map((value) => <option key={value}>{value}</option>)}
        </select>
      </label>
      <label>
        人工状态
        <select value={filters.manualState} onChange={(event) => update("manualState", event.target.value)}>
          <option value="">全部</option>
          {manualStates.map((value) => <option key={value}>{value}</option>)}
        </select>
      </label>
    </section>
  );
}
