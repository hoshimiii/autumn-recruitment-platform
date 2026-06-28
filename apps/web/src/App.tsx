import { useMemo, useState } from "react";
import { emptyJobFilters, filterJobs, getWorkbenchSeed } from "./api/client";
import { JobDetail } from "./features/jobs/JobDetail";
import { JobFilters } from "./features/jobs/JobFilters";
import { JobTable } from "./features/jobs/JobTable";
import { ManualActionForm } from "./features/manual/ManualActionForm";
import { ReviewQueue } from "./features/review/ReviewQueue";
import { SourceList } from "./features/sources/SourceList";
import { TaskList } from "./features/tasks/TaskList";

const navItems = ["Jobs", "Review", "Sources", "Tasks", "Manual"] as const;

export function App() {
  const data = useMemo(() => getWorkbenchSeed(), []);
  const [filters, setFilters] = useState(emptyJobFilters);
  const [selectedJobId, setSelectedJobId] = useState(data.jobs[0]?.id ?? "");
  const filteredJobs = useMemo(() => filterJobs(data.jobs, filters), [data.jobs, filters]);
  const selectedJob = filteredJobs.find((job) => job.id === selectedJobId) ?? filteredJobs[0];

  const summary = [
    { label: "岗位", value: data.jobs.length },
    { label: "待复核", value: data.reviewQueue.length },
    { label: "需人工", value: data.sources.filter((source) => source.needsLogin || source.needsCaptcha).length },
    { label: "任务", value: data.tasks.length }
  ];

  return (
    <div className="app-shell">
      <aside className="sidebar" aria-label="主导航">
        <div className="brand">
          <strong>RecruitOps</strong>
          <span>秋招运营台</span>
        </div>
        <nav>
          {navItems.map((item) => (
            <a key={item} href={`#${item.toLowerCase()}`}>{item}</a>
          ))}
        </nav>
      </aside>

      <main className="workbench" id="jobs">
        <header className="topbar">
          <div>
            <h1>岗位工作台</h1>
            <p>集中查看岗位、来源、复核与人工处理状态。</p>
          </div>
          <div className="toolbar" aria-label="工作台操作">
            <button type="button" className="secondary-button">导入岗位</button>
            <button type="button">创建任务</button>
          </div>
        </header>

        <section className="summary-band" aria-label="概览">
          {summary.map((item) => (
            <div className="summary-item" key={item.label}>
              <span>{item.label}</span>
              <strong>{item.value}</strong>
            </div>
          ))}
        </section>

        <JobFilters jobs={data.jobs} filters={filters} onChange={setFilters} />

        <div className="primary-grid">
          <JobTable jobs={filteredJobs} selectedJobId={selectedJob?.id ?? ""} onSelectJob={setSelectedJobId} />
          {selectedJob ? <JobDetail job={selectedJob} /> : null}
        </div>

        <div className="secondary-grid">
          <ReviewQueue items={data.reviewQueue} />
          <SourceList sources={data.sources} />
          <TaskList tasks={data.tasks} />
          <ManualActionForm />
        </div>
      </main>
    </div>
  );
}
