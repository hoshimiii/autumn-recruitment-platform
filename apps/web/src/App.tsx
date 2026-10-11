import { useEffect, useState, type MouseEvent } from "react";
import { authUrl, loadSession, logout, publicSession } from "./api/auth";
import type { AuthSession, Capability } from "./api/auth";
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
  const [session, setSession] = useState<AuthSession>(publicSession);
  const [authLoaded, setAuthLoaded] = useState(false);
  const [authError, setAuthError] = useState<string>();
  useEffect(() => {
    const controller = new AbortController();
    loadSession(controller.signal).then(setSession).catch(() => {
      if (!controller.signal.aborted) setAuthError("无法加载登录状态。");
    }).finally(() => { if (!controller.signal.aborted) setAuthLoaded(true); });
    return () => controller.abort();
  }, []);
  const allowed = (path: AppRoute) => path === "/" || session.capabilities.includes(requiredCapability[path]);
  const signOut = async () => {
    try { await logout(session); setSession(await loadSession(new AbortController().signal)); navigate("/"); }
    catch { setAuthError("退出失败，请重试。"); }
  };
  const handleNavClick = (event: MouseEvent<HTMLAnchorElement>, path: AppRoute) => { event.preventDefault(); navigate(path); };

  return <div className="recruitment-app app-shell">
    <aside className="sidebar" aria-label="主导航">
      <div className="brand"><strong>RecruitOps</strong><span>秋招运营台</span></div>
      <nav>{navItems.filter((item) => allowed(item.path)).map((item, index) => <a aria-current={routePath === item.path ? "page" : undefined} className={index === 1 ? "nav-start-group" : undefined} href={hrefFor(item.path)} key={item.path} onClick={(event) => handleNavClick(event, item.path)}>{item.label}</a>)}</nav>
      {session.mode === "oidc" && session.loginUrl && !session.authenticated && <a href={authUrl(session.loginUrl)}>登录</a>}
      {session.mode === "oidc" && session.authenticated && <button onClick={() => void signOut()}>退出登录</button>}
      {authError && <p role="alert">{authError}</p>}
    </aside>
    {routePath === "/" && <HomePage hrefFor={hrefFor} onNavigate={navigate} canReview={session.capabilities.includes("REVIEWER")} />}
    {routePath !== "/" && !authLoaded && <main className="workbench" aria-live="polite">正在加载登录状态…</main>}
    {authLoaded && !allowed(routePath) && <main className="workbench" role="alert">此操作需要相应权限。</main>}
    {allowed(routePath) && routePath === "/ops/add" && <OpsAddPage />}
    {allowed(routePath) && routePath === "/ops/review" && <OpsReviewPage />}
    {allowed(routePath) && routePath === "/ops/config" && <OpsConfigPage />}
    {allowed(routePath) && routePath === "/ops/tasks" && <OpsTasksPage />}
  </div>;
}

const requiredCapability: Record<Exclude<AppRoute, "/">, Capability> = {
  "/ops/add": "COLLECTOR", "/ops/review": "REVIEWER", "/ops/config": "ADMIN", "/ops/tasks": "COLLECTOR"
};
