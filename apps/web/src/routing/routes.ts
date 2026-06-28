export const appRoutes = ["/", "/ops/add", "/ops/review", "/ops/config", "/ops/tasks"] as const;

export type AppRoute = (typeof appRoutes)[number];

export type NavItem = {
  group: "display" | "ops";
  label: string;
  path: AppRoute;
};

export const navItems: NavItem[] = [
  { group: "display", label: "岗位展示", path: "/" },
  { group: "ops", label: "添加/导入", path: "/ops/add" },
  { group: "ops", label: "复核", path: "/ops/review" },
  { group: "ops", label: "配置", path: "/ops/config" },
  { group: "ops", label: "任务", path: "/ops/tasks" }
];

export function isAppRoute(path: string): path is AppRoute {
  return appRoutes.includes(path as AppRoute);
}
