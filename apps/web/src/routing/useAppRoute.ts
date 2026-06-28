import { useCallback, useEffect, useMemo, useState } from "react";
import { buildAppHref, getMicroAppBaseRoute, normalizeAppPath } from "../microApp/environment";
import { isAppRoute } from "./routes";
import type { AppRoute } from "./routes";

function resolveRoutePath(baseRoute: string): AppRoute {
  const normalizedPath = normalizeAppPath(window.location.pathname, baseRoute);
  return isAppRoute(normalizedPath) ? normalizedPath : "/";
}

export function useAppRoute() {
  const baseRoute = useMemo(() => getMicroAppBaseRoute(), []);
  const [routePath, setRoutePath] = useState<AppRoute>(() => resolveRoutePath(baseRoute));

  useEffect(() => {
    const handlePopState = () => {
      setRoutePath(resolveRoutePath(baseRoute));
    };

    window.addEventListener("popstate", handlePopState);
    return () => window.removeEventListener("popstate", handlePopState);
  }, [baseRoute]);

  const hrefFor = useCallback((path: AppRoute) => buildAppHref(path, baseRoute), [baseRoute]);

  const navigate = useCallback((path: AppRoute) => {
    window.history.pushState({}, "", buildAppHref(path, baseRoute));
    setRoutePath(path);
  }, [baseRoute]);

  return { hrefFor, navigate, routePath };
}
