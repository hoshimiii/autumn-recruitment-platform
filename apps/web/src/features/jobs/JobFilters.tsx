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

  const companyTypes = uniqueValues(jobs.map((job) => job.companyType));
  const companies = uniqueValues(jobs.map((job) => job.companyName));
  const jobFamilies = uniqueValues(jobs.map((job) => job.jobFamily));
  const jobTypes = uniqueValues(jobs.map((job) => job.jobType));
  const cities = uniqueValues(jobs.map((job) => job.city));
  const sourceTypes = uniqueValues(jobs.map((job) => job.sourceType));
  const statuses = uniqueValues(jobs.map((job) => job.status));
  const techStacks = uniqueValues(jobs.flatMap((job) => job.techStack));
  const certificates = uniqueValues(jobs.flatMap((job) => job.certificateRequirements));
  const deadlineStatuses = uniqueValues(jobs.map((job) => job.deadlineStatus));
  const sourceTrusts = uniqueValues(jobs.map((job) => job.sourceTrust));
  const applicationStatuses = uniqueValues(jobs.map((job) => job.applicationStatus));
  const reviewStatuses = uniqueValues(jobs.map((job) => job.reviewStatus));
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
      <FilterSelect label="公司类型" value={filters.companyType} values={companyTypes} onChange={(value) => update("companyType", value)} />
      <FilterSelect label="公司" value={filters.companyName} values={companies} onChange={(value) => update("companyName", value)} />
      <FilterSelect label="岗位族" value={filters.jobFamily} values={jobFamilies} onChange={(value) => update("jobFamily", value)} />
      <label>
        岗位类型
        <select value={filters.jobType} onChange={(event) => update("jobType", event.target.value)}>
          <option value="">全部</option>
          {jobTypes.map((value) => <option key={value}>{value}</option>)}
        </select>
      </label>
      <FilterSelect label="城市" value={filters.city} values={cities} onChange={(value) => update("city", value)} />
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
      <FilterSelect
        label="证书/资质筛选"
        value={filters.certificate}
        values={certificates}
        onChange={(value) => update("certificate", value)}
      />
      <FilterSelect
        label="截止状态"
        value={filters.deadlineStatus}
        values={deadlineStatuses}
        onChange={(value) => update("deadlineStatus", value)}
      />
      <FilterSelect
        label="来源可信度"
        value={filters.sourceTrust}
        values={sourceTrusts}
        onChange={(value) => update("sourceTrust", value)}
      />
      <FilterSelect
        label="投递状态"
        value={filters.applicationStatus}
        values={applicationStatuses}
        onChange={(value) => update("applicationStatus", value)}
      />
      <FilterSelect
        label="复核状态"
        value={filters.reviewStatus}
        values={reviewStatuses}
        onChange={(value) => update("reviewStatus", value)}
      />
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

type FilterSelectProps = {
  label: string;
  value: string;
  values: string[];
  onChange: (value: string) => void;
};

function FilterSelect({ label, value, values, onChange }: FilterSelectProps) {
  return (
    <label>
      {label}
      <select value={value} onChange={(event) => onChange(event.target.value)}>
        <option value="">全部</option>
        {values.map((item) => <option key={item}>{item}</option>)}
      </select>
    </label>
  );
}
