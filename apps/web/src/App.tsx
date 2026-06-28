import { useMemo, useState } from "react";
import type { MouseEvent } from "react";
import { emptyJobFilters, filterJobs } from "./api/client";
import type { ManualActionInput } from "./api/client";
import { getWorkbenchSeed } from "./api/workbenchSeed";
import { OpsAddPage } from "./pages/OpsAddPage";
import { OpsConfigPage } from "./pages/OpsConfigPage";
import { HomePage } from "./pages/HomePage";
import { OpsReviewPage } from "./pages/OpsReviewPage";
import { OpsTasksPage } from "./pages/OpsTasksPage";
import { navItems } from "./routing/routes";
import { useAppRoute } from "./routing/useAppRoute";
import type { AppRoute } from "./routing/routes";

export function App() {
  const data = useMemo(() => getWorkbenchSeed(), []);
  const { hrefFor, navigate, routePath } = useAppRoute();
  const [filters, setFilters] = useState(emptyJobFilters);
  const [manualRecords, setManualRecords] = useState<ManualActionInput[]>([]);
  const [selectedJobId, setSelectedJobId] = useState(data.jobs[0]?.id ?? "");
  const filteredJobs = useMemo(() => filterJobs(data.jobs, filters), [data.jobs, filters]);
  const selectedJob = filteredJobs.find((job) => job.id === selectedJobId) ?? filteredJobs[0];

  const handleManualSubmit = (record: ManualActionInput) => {
    setManualRecords((current) => [record, ...current]);
  };

  const handleNavClick = (event: MouseEvent<HTMLAnchorElement>, path: AppRoute) => {
    event.preventDefault();
    navigate(path);
  };

  return (
    <div className="app-shell">
      <aside className="sidebar" aria-label="主导航">
        <div className="brand">
          <strong>RecruitOps</strong>
          <span>秋招运营台</span>
        </div>
        <nav>
          {navItems.map((item, index) => (
            <a
              aria-current={routePath === item.path ? "page" : undefined}
              className={index === 1 ? "nav-start-group" : undefined}
              href={hrefFor(item.path)}
              key={item.path}
              onClick={(event) => handleNavClick(event, item.path)}
            >
              {item.label}
            </a>
          ))}
        </nav>
      </aside>

      {routePath === "/" ? (
        <HomePage
          filters={filters}
          filteredJobs={filteredJobs}
          hrefFor={hrefFor}
          jobs={data.jobs}
          onFiltersChange={setFilters}
          onNavigate={navigate}
          onSelectJob={setSelectedJobId}
          selectedJob={selectedJob}
          selectedJobId={selectedJob?.id ?? ""}
        />
      ) : null}

      {routePath === "/ops/add" ? (
        <OpsAddPage manualRecords={manualRecords} onManualSubmit={handleManualSubmit} />
      ) : null}

      {routePath === "/ops/review" ? (
        <OpsReviewPage
          jobs={data.jobs}
          onSelectJob={setSelectedJobId}
          reviewQueue={data.reviewQueue}
          selectedJobId={selectedJobId}
        />
      ) : null}

      {routePath === "/ops/config" ? <OpsConfigPage sources={data.sources} /> : null}
      {routePath === "/ops/tasks" ? <OpsTasksPage tasks={data.tasks} /> : null}
    </div>
  );
}
