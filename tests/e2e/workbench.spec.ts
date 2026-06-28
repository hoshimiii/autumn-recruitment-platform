import { expect, test } from "@playwright/test";

test("workbench shows job trust metadata and visible manual records", async ({ page }) => {
  await page.goto("/");

  await expect(page.getByRole("heading", { name: "岗位工作台" })).toBeVisible();

  const jobTable = page.getByRole("table");
  await expect(jobTable).toContainText("来源可信度");
  await expect(jobTable).toContainText("优先级");
  await expect(jobTable).toContainText("技术栈");
  await expect(jobTable).toContainText("证书/资质");

  const algorithmRow = jobTable.locator("tbody tr").filter({ hasText: "新启智能" });
  await expect(algorithmRow).toContainText("算法工程师");
  await expect(algorithmRow).toContainText("manual");
  await expect(algorithmRow).toContainText("P1");
  await expect(algorithmRow).toContainText("Python, PyTorch, RAG");
  await expect(algorithmRow).toContainText("英语四级, 论文/竞赛加分");

  await algorithmRow.getByRole("button", { name: "算法工程师" }).click();

  const detail = page.getByRole("complementary", { name: "岗位详情" });
  await expect(detail).toContainText("来源 企业微信内推 · 企业微信");
  await expect(detail).toContainText("来源链接 https://wecom.example/referral/newai");
  await expect(detail).toContainText("置信度 64%");
  await expect(detail).toContainText("登录 否");
  await expect(detail).toContainText("验证码 是");
  await expect(detail).toContainText("需处理动作 captcha_required");

  const manualPanel = page.getByRole("region", { name: "人工处理" });
  await expect(manualPanel.getByLabel("平台")).toBeVisible();
  await expect(manualPanel.getByLabel("公司名称")).toBeVisible();
  await expect(manualPanel.getByLabel("技术栈")).toBeVisible();
  await expect(manualPanel.getByLabel("证书/资质")).toBeVisible();

  await manualPanel.getByLabel("平台").fill("BOSS直聘");
  await manualPanel.getByLabel("公司名称").fill("星河制造");
  await manualPanel.getByLabel("岗位名称").fill("后端开发工程师");
  await manualPanel.getByLabel("搜索关键词").fill("Java, 秋招");
  await manualPanel.getByLabel("需处理动作").selectOption("login_required");
  await manualPanel.getByLabel("处理状态").selectOption("pending");
  await manualPanel.getByLabel("投递方式").fill("官网表单");
  await manualPanel.getByLabel("来源链接").fill("https://jobs.example/xinghe-backend");
  await manualPanel.getByLabel("技术栈").fill("Java, Spring");
  await manualPanel.getByLabel("证书/资质").fill("英语四级");
  await manualPanel.getByLabel("优先级").fill("2");
  await manualPanel.getByLabel("需要登录").check();
  await manualPanel.getByLabel("需要验证码").check();
  await manualPanel.getByLabel("处理说明").fill("需要登录后确认投递入口。");
  await manualPanel.getByRole("button", { name: "保存处理记录" }).click();

  const manualRecords = page.getByRole("region", { name: "人工记录" });
  await expect(manualRecords).toContainText("1 条");
  await expect(manualRecords).toContainText("星河制造 · 后端开发工程师");
  await expect(manualRecords).toContainText("login_required · pending");
  await expect(manualRecords).toContainText("P2");
});

test("desktop workbench keeps the job table readable without inner horizontal scroll", async ({ page }) => {
  await page.setViewportSize({ width: 1280, height: 720 });
  await page.goto("/");

  const tableMetrics = await page.locator(".job-table-panel .table-wrap").evaluate((element) => ({
    clientWidth: element.clientWidth,
    rowHeights: Array.from(element.querySelectorAll("tbody tr")).map((row) => row.getBoundingClientRect().height),
    scrollWidth: element.scrollWidth,
    viewportWidth: window.innerWidth,
    bodyScrollWidth: document.documentElement.scrollWidth
  }));
  const detailBox = await page.getByRole("complementary", { name: "岗位详情" }).boundingBox();

  expect(tableMetrics.scrollWidth).toBeLessThanOrEqual(tableMetrics.clientWidth + 1);
  expect(tableMetrics.bodyScrollWidth).toBeLessThanOrEqual(tableMetrics.viewportWidth + 1);
  expect(Math.max(...tableMetrics.rowHeights)).toBeLessThanOrEqual(96);
  expect(detailBox?.width).toBeGreaterThan(300);
});
