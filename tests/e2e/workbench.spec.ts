import { expect, test } from "@playwright/test";

const job = { id: 101, companyCategory: "互联网大厂", companyName: "云澜科技", title: "前端工程师", jobFamily: "开发", jobType: "全职", city: "上海", techStack: ["React", "TypeScript"], certificateRequirements: ["英语六级"], deadline: "2026-10-10", deadlineStatus: "开放", status: "PUBLISHED", priority: 1, sourceName: "云澜官网", sourceType: "HTML", sourceUrl: "https://jobs.example/frontend", sourceTrust: "P1", capturedAt: "2026-07-25T08:00:00Z", updatedAt: "2026-07-25T09:00:00Z", reviewStatus: "APPROVED", applicationStatus: "未投递", jd: "负责 React 前端开发", requirements: "熟悉 TypeScript", applyMethod: "官网", requiresLogin: false, requiresCaptcha: false, confidence: 0.92 };

async function mockApi(page: import("@playwright/test").Page) {
  await page.route("**/api/v1/**", async (route) => {
    const request = route.request(); const path = new URL(request.url()).pathname;
    const body = path === "/api/v1/jobs" ? { items: [job], page: 1, pageSize: 50, total: 1 } : path === "/api/v1/jobs/101" ? job : path === "/api/v1/sources" ? [] : path === "/api/v1/review-items" ? [] : path === "/api/v1/runs" ? [] : path === "/api/v1/manual-items" && request.method() === "GET" ? [] : path === "/api/v1/manual-items" ? { ...await request.postDataJSON(), id: 301 } : {};
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
  await page.setViewportSize({ width: 390, height: 844 });
  const mobile = await page.locator(".app-shell").evaluate((element) => {
    const nav = [...element.querySelectorAll(".sidebar a")];
    return {
      body: document.documentElement.scrollWidth,
      viewport: window.innerWidth,
      navHeights: [...new Set(nav.map((item) => item.getBoundingClientRect().height))],
      clippedNav: nav.some((item) => item.scrollWidth > item.clientWidth + 1)
    };
  });
  expect(mobile.body).toBeLessThanOrEqual(mobile.viewport);
  expect(mobile.navHeights).toHaveLength(1);
  expect(mobile.clippedNav).toBeFalsy();
});

test("real source collection closes the review, display, and export loop", async ({ page, request }) => {
  const api = "http://127.0.0.1:18080/api/v1";
  const sourceResponse = await request.post(`${api}/sources`, {
    data: {
      name: "齐鲁数科校招",
      type: "COMPANY",
      baseUrl: "http://127.0.0.1:14300",
      listUrl: "http://127.0.0.1:14300/jobs",
      adapterType: "JSON",
      mode: "JSON",
      complianceStatus: "ALLOWED",
      trustLevel: "OFFICIAL",
      companyCategory: "SHANDONG_SOE",
      enabled: true,
      termsUrl: "http://127.0.0.1:14300/terms",
      termsConfirmedAt: "2026-07-25T00:00:00Z",
      minIntervalSeconds: 0,
      maxPages: 1,
      maxItems: 10,
      timeoutSeconds: 5,
      selectors: JSON.stringify({ itemsPath: "items" }),
      fieldMapping: JSON.stringify({
        companyName: "company", title: "title", city: "city", jobFamily: "jobFamily",
        jobType: "jobType", techStack: "techStack", certificateRequirements: "certificates",
        deadline: "deadline", jd: "jd", requirements: "requirements", sourceUrl: "url",
        applyUrl: "applyUrl", sourceTrust: "$const:OFFICIAL"
      })
    }
  });
  expect(sourceResponse.ok()).toBeTruthy();
  const source = await sourceResponse.json() as { id: number };
  const run = await request.post(`${api}/sources/${source.id}/runs`);
  expect(run.ok()).toBeTruthy();
  expect(await run.json()).toMatchObject({ status: "SUCCESS", discoveredCount: 1, reviewItemCount: 1 });

  await page.goto("/ops/review");
  await expect(page.getByText("齐鲁数科 · Java 开发工程师")).toBeVisible();
  await page.getByRole("button", { name: "通过" }).click();
  await expect(page.getByText("当前没有待复核岗位。")).toBeVisible();

  await page.goto("/");
  await expect(page.getByRole("button", { name: "Java 开发工程师" })).toBeVisible();
  await page.getByLabel("技术栈").selectOption("Java");
  await expect(page).toHaveURL(/techStack=Java/);
  await page.getByRole("button", { name: "Java 开发工程师" }).click();
  await expect(page.getByRole("complementary", { name: "岗位详情" })).toContainText("Spring Boot");

  const markdown = await request.get(`${api}/exports/jobs.md?techStack=Java`);
  expect(markdown.ok()).toBeTruthy();
  expect(await markdown.text()).toContain("齐鲁数科");
  const workbook = await request.get(`${api}/exports/jobs.xlsx?techStack=Java`);
  expect(workbook.ok()).toBeTruthy();
  expect((await workbook.body()).subarray(0, 2).toString()).toBe("PK");
});
