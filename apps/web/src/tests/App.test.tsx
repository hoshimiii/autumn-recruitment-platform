import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { App } from "../App";
import { getApiBaseUrl } from "../api/client";

const job = {
  id: "job-1", companyCategory: "互联网大厂", companyName: "云澜科技", title: "前端工程师",
  jobFamily: "开发", jobType: "全职", city: "上海", techStack: ["React", "TypeScript"],
  certificateRequirements: ["英语六级"], deadline: "2026-10-10", deadlineStatus: "开放",
  status: "PUBLISHED", priority: 1, sourceName: "云澜官网", sourceType: "HTML",
  sourceUrl: "https://jobs.example/frontend", sourceTrust: "P1", capturedAt: "2026-07-25T08:00:00Z",
  updatedAt: "2026-07-25T09:00:00Z", reviewStatus: "APPROVED", applicationStatus: "未投递",
  jd: "负责 React 前端开发", requirements: ["熟悉 TypeScript"], applyUrl: "https://jobs.example/apply",
  applyMethod: "官网", requiresLogin: false, requiresCaptcha: false, confidence: 0.92, snapshots: []
};

function json(body: unknown, status = 200): Response {
  return new Response(JSON.stringify(body), { status, headers: { "Content-Type": "application/json" } });
}

function mockApi(overrides: Record<string, Response> = {}) {
  return vi.spyOn(globalThis, "fetch").mockImplementation((input) => {
    const url = String(input);
    if (overrides[url]) return Promise.resolve(overrides[url]);
    if (url.includes("/jobs/job-1")) return Promise.resolve(json(job));
    if (url.includes("/jobs")) return Promise.resolve(json({ items: [job], page: 1, pageSize: 50, total: 1 }));
    if (url.includes("/sources")) return Promise.resolve(json([]));
    if (url.includes("/review-items")) return Promise.resolve(json([]));
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
    await waitFor(() => expect(fetchMock).toHaveBeenCalledWith(
      expect.stringContaining("/api/v1/jobs"), expect.objectContaining({ signal: expect.any(AbortSignal) })
    ));

    fireEvent.click(screen.getByRole("button", { name: "前端工程师" }));
    expect(await screen.findByText("负责 React 前端开发")).toBeInTheDocument();
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
    fireEvent.click(screen.getByLabelText("启用 AI"));
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
    fireEvent.click(screen.getByRole("button", { name: "保存来源" }));

    await waitFor(() => expect(fetchMock).toHaveBeenCalledWith(
      "/api/v1/sources",
      expect.objectContaining({ method: "POST", body: expect.stringContaining('"enabled":false') })
    ));
  });

  it("uses the host-provided API base URL", () => {
    window.__RECRUITMENT_API_BASE_URL__ = "http://127.0.0.1:8080/api/v1/";
    expect(getApiBaseUrl()).toBe("http://127.0.0.1:8080/api/v1");
    delete window.__RECRUITMENT_API_BASE_URL__;
  });
});
