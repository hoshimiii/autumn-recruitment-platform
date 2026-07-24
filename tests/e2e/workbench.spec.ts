import { expect, test } from "@playwright/test";

const job = { id: "job-1", companyCategory: "互联网大厂", companyName: "云澜科技", title: "前端工程师", jobFamily: "开发", jobType: "全职", city: "上海", techStack: ["React", "TypeScript"], certificateRequirements: ["英语六级"], deadline: "2026-10-10", deadlineStatus: "开放", status: "PUBLISHED", priority: 1, sourceName: "云澜官网", sourceType: "HTML", sourceUrl: "https://jobs.example/frontend", sourceTrust: "P1", capturedAt: "2026-07-25T08:00:00Z", updatedAt: "2026-07-25T09:00:00Z", reviewStatus: "APPROVED", applicationStatus: "未投递", jd: "负责 React 前端开发", requirements: ["熟悉 TypeScript"], applyMethod: "官网", requiresLogin: false, requiresCaptcha: false, confidence: 0.92 };

async function mockApi(page: import("@playwright/test").Page) {
  await page.route("**/api/v1/**", async (route) => {
    const request = route.request(); const path = new URL(request.url()).pathname;
    const body = path === "/api/v1/jobs" ? { items: [job], page: 1, pageSize: 50, total: 1 } : path === "/api/v1/jobs/job-1" ? job : path === "/api/v1/sources" ? [] : path === "/api/v1/review-items" ? [] : path === "/api/v1/runs" ? [] : path === "/api/v1/manual-items" && request.method() === "GET" ? [] : path === "/api/v1/manual-items" ? { ...await request.postDataJSON(), id: "manual-1" } : {};
    await route.fulfill({ contentType: "application/json", body: JSON.stringify(body) });
  });
}

test("home fetches jobs, keeps filters in URL, and shows details", async ({ page }) => {
  await mockApi(page); await page.goto("/");
  const table = page.getByRole("table");
  await expect(table).toContainText("来源可信度");
  await expect(table).toContainText("前端工程师");
  await page.getByLabel("关键词").fill("云澜");
  await expect(page).toHaveURL(/keyword=/);
  await page.getByRole("button", { name: "前端工程师" }).click();
  await expect(page.getByRole("complementary", { name: "岗位详情" })).toContainText("负责 React 前端开发");
  await expect(page.getByLabel("导出 Markdown")).toHaveAttribute("href", /exports\/jobs\.md/);
});

test("ops routes stay separate and keep a compact desktop layout", async ({ page }) => {
  await mockApi(page); await page.setViewportSize({ width: 1280, height: 720 });
  for (const route of [{ path: "/ops/add", title: "添加/导入" }, { path: "/ops/review", title: "岗位复核" }, { path: "/ops/config", title: "来源配置" }, { path: "/ops/tasks", title: "运行记录" }]) {
    await page.goto(route.path); await expect(page.locator("h1", { hasText: route.title })).toBeVisible();
  }
  await page.goto("/");
  const metrics = await page.locator(".job-table-panel .table-wrap").evaluate((element) => ({ client: element.clientWidth, scroll: element.scrollWidth, body: document.documentElement.scrollWidth, viewport: window.innerWidth }));
  expect(metrics.scroll).toBeLessThanOrEqual(metrics.client + 1);
  expect(metrics.body).toBeLessThanOrEqual(metrics.viewport + 1);
});
