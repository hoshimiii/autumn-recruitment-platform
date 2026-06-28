export type MicroAppGlobals = {
  __MICRO_APP_BASE_ROUTE__?: string;
  __MICRO_APP_ENVIRONMENT__?: boolean;
  __MICRO_APP_NAME__?: string;
  __MICRO_APP_PUBLIC_PATH__?: string;
};

export function isMicroAppEnvironment(target: MicroAppGlobals = window): boolean {
  return target.__MICRO_APP_ENVIRONMENT__ === true;
}

export function normalizeBaseRoute(route?: string): string {
  const value = route?.trim();

  if (!value || value === "/") {
    return "";
  }

  const withLeadingSlash = value.startsWith("/") ? value : `/${value}`;
  return withLeadingSlash.replace(/\/+$/u, "");
}

export function getMicroAppBaseRoute(target: MicroAppGlobals = window): string {
  return normalizeBaseRoute(target.__MICRO_APP_BASE_ROUTE__);
}

export function normalizeAppPath(pathname: string, baseRoute = getMicroAppBaseRoute()): string {
  const path = pathname.startsWith("/") ? pathname : `/${pathname}`;
  const base = normalizeBaseRoute(baseRoute);

  if (!base) {
    return path;
  }

  if (path === base) {
    return "/";
  }

  if (path.startsWith(`${base}/`)) {
    return path.slice(base.length) || "/";
  }

  return path;
}

export function buildAppHref(appPath: string, baseRoute = getMicroAppBaseRoute()): string {
  const route = appPath.startsWith("/") ? appPath : `/${appPath}`;
  const base = normalizeBaseRoute(baseRoute);

  if (route === "/") {
    return base || "/";
  }

  return `${base}${route}`;
}
