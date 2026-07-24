import type { Job, JobFilters as State } from "../../api/client";

type Props = { jobs: Job[]; filters: State; onChange: (filters: State) => void };

export function JobFilters({ jobs, filters, onChange }: Props) {
  const update = (key: keyof State, value: string) => onChange({ ...filters, [key]: value });
  const values = (items: string[]) => [...new Set(items.filter(Boolean))].sort((a, b) => a.localeCompare(b, "zh-CN"));
  const select = (label: string, key: keyof State, items: string[]) => <label>{label}<select aria-label={label} value={filters[key]} onChange={(event) => update(key, event.target.value)}><option value="">全部</option>{values(items).map((item) => <option key={item} value={item}>{item}</option>)}</select></label>;
  return <section className="panel filter-panel" aria-label="岗位筛选">
    <label>关键词<input value={filters.keyword} onChange={(event) => update("keyword", event.target.value)} placeholder="公司、岗位、城市" /></label>
    {select("公司类型", "companyCategory", jobs.map((job) => job.companyCategory))}
    {select("公司", "company", jobs.map((job) => job.companyName))}
    {select("岗位类型", "jobType", jobs.map((job) => job.jobType))}
    {select("城市", "city", jobs.map((job) => job.city))}
    {select("技术栈筛选", "techStack", jobs.flatMap((job) => job.techStack))}
    {select("证书/资质筛选", "certificate", jobs.flatMap((job) => job.certificateRequirements))}
    <label>截止日期从<input aria-label="截止日期从" type="date" value={filters.deadlineFrom} onChange={(event) => update("deadlineFrom", event.target.value)} /></label>
    <label>截止日期至<input aria-label="截止日期至" type="date" value={filters.deadlineTo} onChange={(event) => update("deadlineTo", event.target.value)} /></label>
    {select("来源可信度", "sourceTrust", jobs.map((job) => job.sourceTrust))}
  </section>;
}
