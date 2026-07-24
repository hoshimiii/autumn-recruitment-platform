import type { MouseEvent } from "react";
import { HomePage } from "./pages/HomePage";
import { OpsAddPage } from "./pages/OpsAddPage";
import { OpsConfigPage } from "./pages/OpsConfigPage";
import { OpsReviewPage } from "./pages/OpsReviewPage";
import { OpsTasksPage } from "./pages/OpsTasksPage";
import { navItems } from "./routing/routes";
import { useAppRoute } from "./routing/useAppRoute";
import type { AppRoute } from "./routing/routes";

export function App() {
  const { hrefFor, navigate, routePath } = useAppRoute();
  const handleNavClick = (event: MouseEvent<HTMLAnchorElement>, path: AppRoute) => { event.preventDefault(); navigate(path); };

  return <div className="recruitment-app app-shell">
    <aside className="sidebar" aria-label="主导航">
      <div className="brand"><strong>RecruitOps</strong><span>秋招运营台</span></div>
      <nav>{navItems.map((item, index) => <a aria-current={routePath === item.path ? "page" : undefined} className={index === 1 ? "nav-start-group" : undefined} href={hrefFor(item.path)} key={item.path} onClick={(event) => handleNavClick(event, item.path)}>{item.label}</a>)}</nav>
    </aside>
    {routePath === "/" && <HomePage hrefFor={hrefFor} onNavigate={navigate} />}
    {routePath === "/ops/add" && <OpsAddPage />}
    {routePath === "/ops/review" && <OpsReviewPage />}
    {routePath === "/ops/config" && <OpsConfigPage />}
    {routePath === "/ops/tasks" && <OpsTasksPage />}
  </div>;
}
