import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { App } from "../App";
import { getApiBaseUrl } from "../api/client";
import { JobDetail } from "../features/jobs/JobDetail";

const job = {
  id: 101, companyCategory: "互联网大厂", companyName: "云澜科技", title: "前端工程师",
  jobFamily: "开发", jobType: "全职", city: "上海", techStack: ["React", "TypeScript"],
  certificateRequirements: ["英语六级"], deadline: "2026-10-10", deadlineStatus: "开放",
  status: "PUBLISHED", priority: 1, sourceName: "云澜官网", sourceType: "HTML",
  sourceUrl: "https://jobs.example/frontend", sourceTrust: "OFFICIAL", capturedAt: "2026-07-25T08:00:00Z",
  updatedAt: "2026-07-25T09:00:00Z", reviewStatus: "APPROVED", applicationStatus: "未投递",
  jd: "负责 React 前端开发", requirements: "熟悉 TypeScript\n具备工程化经验", applyUrl: "https://jobs.example/apply",
  applyMethod: "官网", requiresLogin: false, requiresCaptcha: false, confidence: 0.92, snapshots: []
};

const source = {
  id: 11, name: "示例招聘官网", type: "COMPANY", baseUrl: "https://jobs.example",
  listUrl: "https://jobs.example/api/jobs", adapterType: "JSON", mode: "JSON",
  complianceStatus: "ALLOWED", trustLevel: "P1", companyCategory: "INTERNET_BIG_TECH",
  enabled: false, cron: "0 0 8 * * *", timezone: "Asia/Shanghai", minIntervalSeconds: 5,
  maxPages: 2, maxItems: 30, selectors: "{}", fieldMapping: "{}", termsUrl: "https://jobs.example/terms",
  termsConfirmedAt: "2026-07-25T00:00:00Z", robotsUrl: "https://jobs.example/robots.txt", timeoutSeconds: 15,
  etag: "etag-1", lastModified: "Fri, 25 Jul 2026 00:00:00 GMT", lastRunAt: undefined,
  lastStatus: undefined, aiEnabled: false, aiProvider: undefined, aiModel: undefined, configVersion: 3
};

function json(body: unknown, status = 200): Response {
  return new Response(JSON.stringify(body), { status, headers: { "Content-Type": "application/json" } });
}

function mockApi(overrides: Record<string, Response> = {}, reviewItems: unknown[] = []) {
  return vi.spyOn(globalThis, "fetch").mockImplementation((input, init) => {
    const url = String(input);
    if (overrides[`${init?.method ?? "GET"} ${url}`] ?? overrides[url]) return Promise.resolve(overrides[`${init?.method ?? "GET"} ${url}`] ?? overrides[url]);
    if (url.includes("/jobs/101")) return Promise.resolve(json(job));
    if (url.includes("/jobs")) return Promise.resolve(json({ items: [job], page: 1, pageSize: 50, total: 1 }));
    if (url.includes("/sources")) return Promise.resolve(json([]));
    if (url.includes("/review-items/") && init?.method === "POST") return Promise.resolve(new Response(null, { status: 204 }));
    if (/\/review-items\/\d+$/u.test(url)) return Promise.resolve(json({
      ...(reviewItems[0] as Record<string, unknown>),
      sourceName: "云澜官网",
      sourceUrl: "https://jobs.example/frontend",
      candidatePayload: { title: "前端工程师", raw: "原始岗位描述" },
      normalizedPayload: { companyName: "云澜科技", title: "前端工程师" },
      dedupeKey: "yunlan-frontend",
      contentHash: "hash-202"
    }));
    if (url.includes("/review-items")) return Promise.resolve(json(reviewItems));
    if (url.includes("/runs")) return Promise.resolve(json([]));
    if (url.includes("/manual-items")) return Promise.resolve(json([]));
    return Promise.resolve(json({}));
  });
}

describe("API-driven micro frontend", () => {
  beforeEach(() => {
    window.history.replaceState({}, "", "/");
  });

  afterEach(() => vi.restoreAllMocks());

  it("loads published jobs from REST and fetches the selected detail", async () => {
    const fetchMock = mockApi();
    render(<App />);

    expect(await screen.findByRole("button", { name: "前端工程师" })).toBeInTheDocument();
    expect(screen.getByLabelText("岗位概览")).toHaveTextContent("开放1");
    expect(screen.getByLabelText("岗位概览")).toHaveTextContent("官方来源1");
    expect(screen.getByRole("option", { name: "官网" })).toBeInTheDocument();
    expect(screen.getAllByText("已发布")).not.toHaveLength(0);
    await waitFor(() => expect(fetchMock).toHaveBeenCalledWith(
      expect.stringContaining("/api/v1/jobs"), expect.objectContaining({ signal: expect.any(AbortSignal) })
    ));

    fireEvent.click(screen.getByRole("button", { name: "前端工程师" }));
    expect(await screen.findByText("负责 React 前端开发")).toBeInTheDocument();
    expect(screen.getByText(/熟悉 TypeScript\s+具备工程化经验/)).toHaveClass("requirements-text");
  });

  it("shows an empty state when job requirements are missing", () => {
    render(<JobDetail job={{ ...job, requirements: undefined }} />);
    expect(screen.getByText("暂无岗位要求。")).toBeInTheDocument();
  });

  it("persists filters in the URL and restores them after remount", async () => {
    mockApi();
    const view = render(<App />);
    await screen.findByRole("button", { name: "前端工程师" });

    fireEvent.change(screen.getByLabelText("关键词"), { target: { value: "云澜" } });
    expect(window.location.search).toContain("keyword=%E4%BA%91%E6%BE%9C");
    view.unmount();
    render(<App />);

    expect(screen.getByLabelText("关键词")).toHaveValue("云澜");
  });

  it("renders visible empty and API error states", async () => {
    mockApi({ "/api/v1/jobs?page=1&pageSize=50&sort=updatedAt%2Cdesc": json({ items: [], page: 1, pageSize: 50, total: 0 }) });
    const view = render(<App />);
    expect(await screen.findByText("暂无已发布岗位。")).toBeInTheDocument();
    view.unmount();

    vi.restoreAllMocks();
    mockApi({ "/api/v1/jobs?page=1&pageSize=50&sort=updatedAt%2Cdesc": json({ error: { code: "database_unavailable", message: "数据库不可用" } }, 503) });
    render(<App />);
    expect(await screen.findByRole("alert")).toHaveTextContent("数据库不可用");
  });

  it("validates agent secret references and never accepts a plaintext token", async () => {
    mockApi();
    window.history.replaceState({}, "", "/ops/config");
    render(<App />);
    await screen.findByRole("heading", { name: "来源配置" });

    fireEvent.click(screen.getByRole("button", { name: "新增来源" }));
    fireEvent.change(screen.getByLabelText("来源名称"), { target: { value: "官网" } });
    fireEvent.change(screen.getByLabelText("官网 URL"), { target: { value: "https://jobs.example" } });
    fireEvent.change(screen.getByLabelText("列表 URL"), { target: { value: "https://jobs.example/jobs" } });
    fireEvent.change(screen.getByLabelText("企业分类"), { target: { value: "INTERNET_BIG_TECH" } });
    fireEvent.click(screen.getByLabelText("记录 AI 配置"));
    fireEvent.change(screen.getByLabelText("密钥环境变量引用"), { target: { value: "plain-token" } });
    fireEvent.click(screen.getByRole("button", { name: "保存来源" }));

    expect(await screen.findByRole("alert")).toHaveTextContent("环境变量引用");
  });

  it("keeps an unknown new source disabled when it is saved", async () => {
    const fetchMock = mockApi();
    window.history.replaceState({}, "", "/ops/config");
    render(<App />);
    await screen.findByRole("heading", { name: "来源配置" });

    fireEvent.click(screen.getByRole("button", { name: "新增来源" }));
    expect(screen.getByLabelText("启用来源")).not.toBeChecked();
    fireEvent.change(screen.getByLabelText("来源名称"), { target: { value: "待确认官网" } });
    fireEvent.change(screen.getByLabelText("官网 URL"), { target: { value: "https://jobs.example" } });
    fireEvent.change(screen.getByLabelText("列表 URL"), { target: { value: "https://jobs.example/jobs" } });
    fireEvent.change(screen.getByLabelText("企业分类"), { target: { value: "INTERNET_BIG_TECH" } });
    fireEvent.click(screen.getByRole("button", { name: "保存来源" }));

    await waitFor(() => expect(fetchMock).toHaveBeenCalledWith(
      "/api/v1/sources",
      expect.objectContaining({ method: "POST", body: expect.stringContaining('"enabled":false') })
    ));
  });

  it("sends the Java source DTO and converts confirmed terms time to ISO", async () => {
    const fetchMock = mockApi({ "POST /api/v1/sources": json({ ...source, id: 12 }, 201) });
    window.history.replaceState({}, "", "/ops/config");
    render(<App />);
    await screen.findByRole("heading", { name: "来源配置" });

    fireEvent.click(screen.getByRole("button", { name: "新增来源" }));
    fireEvent.change(screen.getByLabelText("来源名称"), { target: { value: "腾讯招聘" } });
    fireEvent.change(screen.getByLabelText("官网 URL"), { target: { value: "https://careers.example" } });
    fireEvent.change(screen.getByLabelText("列表 URL"), { target: { value: "https://careers.example/jobs" } });
    fireEvent.change(screen.getByLabelText("企业分类"), { target: { value: "INTERNET_BIG_TECH" } });
    fireEvent.change(screen.getByLabelText("最大条目数"), { target: { value: "42" } });
    fireEvent.change(screen.getByLabelText("条款确认时间"), { target: { value: "2026-07-25T08:30" } });
    fireEvent.click(screen.getByRole("button", { name: "保存来源" }));

    await waitFor(() => expect(fetchMock).toHaveBeenCalledWith(
      "/api/v1/sources",
      expect.objectContaining({ method: "POST", body: expect.stringContaining('"listUrl":"https://careers.example/jobs"') })
    ));
    const body = JSON.parse(String(fetchMock.mock.calls.find(([, init]) => init?.method === "POST")?.[1]?.body));
    expect(body).toMatchObject({ maxItems: 42, enabled: false, complianceStatus: "UNKNOWN" });
    expect(body.termsConfirmedAt).toMatch(/Z$/u);
    expect(body).not.toHaveProperty("aiSecretRef");
  });

  it("only permits enablement after allowed policy and confirmed terms", async () => {
    mockApi();
    window.history.replaceState({}, "", "/ops/config");
    render(<App />);
    await screen.findByRole("heading", { name: "来源配置" });
    fireEvent.click(screen.getByRole("button", { name: "新增来源" }));

    expect(screen.getByLabelText("启用来源")).toBeDisabled();
    fireEvent.change(screen.getByLabelText("合规状态"), { target: { value: "ALLOWED" } });
    fireEvent.change(screen.getByLabelText("条款确认时间"), { target: { value: "2026-07-25T08:30" } });
    expect(screen.getByLabelText("启用来源")).toBeDisabled();
    fireEvent.change(screen.getByLabelText("条款 URL"), { target: { value: "https://jobs.example/terms" } });
    expect(screen.getByLabelText("启用来源")).toBeEnabled();
  });

  it("resets source form values when switching the edited source", async () => {
    const second = { ...source, id: 12, name: "第二来源", baseUrl: "https://second.example",
      listUrl: "https://second.example/jobs", termsUrl: "https://second.example/terms" };
    mockApi({ "/api/v1/sources": json([source, second]) });
    window.history.replaceState({}, "", "/ops/config");
    render(<App />);

    fireEvent.click(await screen.findByRole("button", { name: "编辑来源 示例招聘官网" }));
    expect(screen.getByLabelText("来源名称")).toHaveValue("示例招聘官网");
    fireEvent.click(screen.getByRole("button", { name: "编辑来源 第二来源" }));
    expect(screen.getByLabelText("来源名称")).toHaveValue("第二来源");
    expect(screen.getByLabelText("官网 URL")).toHaveValue("https://second.example");
  });

  it("does not require parser JSON for manual sources", async () => {
    mockApi();
    window.history.replaceState({}, "", "/ops/config");
    render(<App />);
    await screen.findByRole("heading", { name: "来源配置" });
    fireEvent.click(screen.getByRole("button", { name: "新增来源" }));
    fireEvent.change(screen.getByLabelText("采集模式"), { target: { value: "MANUAL" } });
    expect(screen.queryByLabelText("Selectors")).not.toBeInTheDocument();
    expect(screen.queryByLabelText("字段映射")).not.toBeInTheDocument();
  });

  it("calls test, run, update and delete source endpoints", async () => {
    vi.spyOn(window, "confirm").mockReturnValue(true);
    const fetchMock = mockApi({
      "/api/v1/sources": json([source]),
      "POST /api/v1/sources/11/test": json({ id: 1, status: "SUCCESS" }),
      "POST /api/v1/sources/11/runs": json({ id: 2, status: "SUCCESS" }),
      "PUT /api/v1/sources/11": json(source),
      "DELETE /api/v1/sources/11": new Response(null, { status: 204 })
    });
    window.history.replaceState({}, "", "/ops/config");
    render(<App />);
    expect(await screen.findByText("示例招聘官网")).toBeInTheDocument();

    fireEvent.click(screen.getByRole("button", { name: "测试来源 示例招聘官网" }));
    await waitFor(() => expect(fetchMock).toHaveBeenCalledWith(
      "/api/v1/sources/11/test", expect.objectContaining({ method: "POST" })
    ));
    fireEvent.click(screen.getByRole("button", { name: "运行来源 示例招聘官网" }));
    await waitFor(() => expect(fetchMock).toHaveBeenCalledWith(
      "/api/v1/sources/11/runs", expect.objectContaining({ method: "POST" })
    ));
    fireEvent.click(screen.getByRole("button", { name: "编辑来源 示例招聘官网" }));
    fireEvent.click(screen.getByRole("button", { name: "保存来源" }));
    await waitFor(() => expect(fetchMock).toHaveBeenCalledWith(
      "/api/v1/sources/11", expect.objectContaining({ method: "PUT" })
    ));
    fireEvent.click(screen.getByRole("button", { name: "删除来源 示例招聘官网" }));

    await waitFor(() => expect(fetchMock).toHaveBeenCalledWith(
      "/api/v1/sources/11", expect.objectContaining({ method: "DELETE" })
    ));
  });

  it("shows source action API errors", async () => {
    mockApi({ "/api/v1/sources": json([source]), "/api/v1/sources/11/test": json({ error: { message: "条款尚未确认" } }, 422) });
    window.history.replaceState({}, "", "/ops/config");
    render(<App />);
    fireEvent.click(await screen.findByRole("button", { name: "测试来源 示例招聘官网" }));
    expect(await screen.findByRole("alert")).toHaveTextContent("条款尚未确认");
  });

  it("sends a non-empty reason when blocking a numeric review item", async () => {
    const fetchMock = mockApi({}, [{
      id: 202,
      companyName: "云澜科技",
      title: "前端工程师",
      status: "PENDING_REVIEW",
      confidence: 0.8
    }]);
    window.history.replaceState({}, "", "/ops/review");
    render(<App />);

    fireEvent.click(await screen.findByRole("button", { name: "查看证据" }));
    await waitFor(() => expect(screen.getAllByText((_text, element) =>
      element?.tagName === "PRE" && Boolean(element.textContent?.includes("原始岗位描述"))
    )).not.toHaveLength(0));
    expect(screen.getByRole("link", { name: "原始链接" })).toHaveAttribute(
      "href", "https://jobs.example/frontend"
    );
    fireEvent.click(await screen.findByRole("button", { name: "人工跟进" }));

    await waitFor(() => expect(fetchMock).toHaveBeenCalledWith(
      "/api/v1/review-items/202/block",
      expect.objectContaining({
        method: "POST",
        body: JSON.stringify({ reason: "manual_follow_up" })
      })
    ));
  });

  it("uses the host-provided API base URL", () => {
    window.__RECRUITMENT_API_BASE_URL__ = "http://127.0.0.1:8080/api/v1/";
    expect(getApiBaseUrl()).toBe("http://127.0.0.1:8080/api/v1");
    delete window.__RECRUITMENT_API_BASE_URL__;
  });
});
