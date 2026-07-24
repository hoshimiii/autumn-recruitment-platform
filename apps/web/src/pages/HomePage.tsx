import { useEffect, useMemo, useState } from "react";
import { Download } from "lucide-react";
import { ApiError, emptyJobFilters, exportHref, getJob, listJobs, readJobFilters, writeJobFilters } from "../api/client";
import type { EntityId, Job, JobFilters as JobFilterState } from "../api/client";
import { JobDetail } from "../features/jobs/JobDetail";
import { JobFilters } from "../features/jobs/JobFilters";
import { JobTable } from "../features/jobs/JobTable";
import { PageHeader } from "../features/layout/PageHeader";
import type { AppRoute } from "../routing/routes";

type Props = { hrefFor: (path: AppRoute) => string; onNavigate: (path: AppRoute) => void };

export function HomePage({ hrefFor, onNavigate }: Props) {
  const [filters, setFilters] = useState<JobFilterState>(() => readJobFilters());
  const [jobs, setJobs] = useState<Job[]>([]);
  const [selectedId, setSelectedId] = useState<EntityId | null>(null);
  const [detail, setDetail] = useState<Job>();
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string>();

  useEffect(() => {
    const controller = new AbortController();
    setLoading(true); setError(undefined);
    listJobs(filters, controller.signal).then((page) => {
      setJobs(page.items);
      setSelectedId((current) => page.items.some((job) => job.id === current) ? current : (page.items[0]?.id ?? null));
    }).catch((reason: unknown) => { if (!controller.signal.aborted) setError(reason instanceof ApiError ? reason.message : "无法加载岗位数据。"); }).finally(() => { if (!controller.signal.aborted) setLoading(false); });
    return () => controller.abort();
  }, [filters]);

  useEffect(() => {
    if (selectedId === null) { setDetail(undefined); return; }
    const controller = new AbortController();
    getJob(selectedId, controller.signal).then(setDetail).catch((reason: unknown) => { if (!controller.signal.aborted) setError(reason instanceof ApiError ? reason.message : "无法加载岗位详情。"); });
    return () => controller.abort();
  }, [selectedId]);

  const selected = detail?.id === selectedId ? detail : jobs.find((job) => job.id === selectedId);
  const summary = useMemo(() => [
    ["岗位", jobs.length], ["开放", jobs.filter(open).length],
    ["今日更新", jobs.filter((job) => recent(job.updatedAt ?? job.capturedAt)).length],
    ["官方来源", jobs.filter((job) => job.sourceTrust === "OFFICIAL").length]
  ], [jobs]);
  const updateFilters = (next: JobFilterState) => { writeJobFilters(next); setFilters(next); };

  return <main className="workbench">
    <PageHeader title="岗位展示" description="每日已发布岗位，按条件筛选并查看完整来源证据。" actions={<>
      <a className="icon-button secondary-button" aria-label="导出 Markdown" href={exportHref("md", filters)} title="导出 Markdown"><Download size={16} /></a>
      <a className="icon-button secondary-button" aria-label="导出 Excel" href={exportHref("xlsx", filters)} title="导出 Excel"><Download size={16} /></a>
      <a className="button-link" href={hrefFor("/ops/review")} onClick={(event) => { event.preventDefault(); onNavigate("/ops/review"); }}>进入复核</a>
    </>} />
    <section className="summary-band" aria-label="岗位概览">{summary.map(([label, value]) => <div className="summary-item" key={label}><span>{label}</span><strong>{value}</strong></div>)}</section>
    <JobFilters jobs={jobs} filters={filters} onChange={updateFilters} />
    {error ? <section className="panel state-panel" role="alert"><strong>岗位数据不可用</strong><span>{error}</span><button type="button" onClick={() => setFilters({ ...filters })}>重试</button></section> : null}
    {loading ? <section className="panel state-panel" aria-live="polite">正在加载岗位数据…</section> : null}
    {!loading && jobs.some((job) => stale(job.updatedAt ?? job.capturedAt)) ? <section className="panel state-panel" aria-live="polite">部分岗位超过 24 小时未更新，数据可能已过期。</section> : null}
    {!loading && !error && jobs.length === 0 ? <section className="panel state-panel">暂无已发布岗位。</section> : null}
    {jobs.length > 0 ? <div className="primary-grid"><JobTable jobs={jobs} selectedJobId={selectedId} onSelectJob={setSelectedId} />{selected ? <JobDetail job={selected} /> : null}</div> : null}
  </main>;
}

function recent(value?: string): boolean { return Boolean(value && Date.now() - new Date(value).getTime() < 24 * 60 * 60 * 1000); }
function stale(value?: string): boolean { return Boolean(value && Date.now() - new Date(value).getTime() >= 24 * 60 * 60 * 1000); }
function open(job: Job): boolean { return !job.deadline || job.deadline >= new Date().toISOString().slice(0, 10); }
