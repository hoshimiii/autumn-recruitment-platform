import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { App } from "../App";
import { ManualActionForm } from "../features/manual/ManualActionForm";
import { SourceList } from "../features/sources/SourceList";

describe("App", () => {
  beforeEach(() => {
    window.history.pushState({}, "", "/");
  });

  it("renders the home job display with tech stack and certificate columns", () => {
    render(<App />);

    expect(screen.getByRole("heading", { name: "岗位展示" })).toBeInTheDocument();
    expect(screen.getByText("技术栈")).toBeInTheDocument();
    expect(screen.getByText("证书/资质")).toBeInTheDocument();
  });

  it("keeps operational task, review, source and manual panels out of the home display", () => {
    render(<App />);

    expect(screen.queryByRole("heading", { name: "任务" })).not.toBeInTheDocument();
    expect(screen.queryByRole("heading", { name: "复核队列" })).not.toBeInTheDocument();
    expect(screen.queryByRole("heading", { name: "来源配置" })).not.toBeInTheDocument();
    expect(screen.queryByRole("heading", { name: "人工处理" })).not.toBeInTheDocument();
  });

  it("renders manual import and add work under the ops add route", () => {
    window.history.pushState({}, "", "/ops/add");
    render(<App />);

    expect(screen.getByRole("heading", { name: "添加/导入" })).toBeInTheDocument();
    expect(screen.getByRole("heading", { name: "人工处理" })).toBeInTheDocument();
    expect(screen.getByLabelText("需要登录")).toBeInTheDocument();
    expect(screen.getByLabelText("需要验证码")).toBeInTheDocument();
  });

  it("renders review and source configuration under separate ops child routes", () => {
    window.history.pushState({}, "", "/ops/review");
    const { rerender } = render(<App />);

    expect(screen.getByRole("heading", { name: "岗位复核" })).toBeInTheDocument();
    expect(screen.getByRole("heading", { name: "复核队列" })).toBeInTheDocument();
    expect(screen.queryByRole("heading", { name: "来源配置" })).not.toBeInTheDocument();

    window.history.pushState({}, "", "/ops/config");
    window.dispatchEvent(new PopStateEvent("popstate"));
    rerender(<App />);

    expect(screen.getByRole("heading", { name: "来源配置" })).toBeInTheDocument();
    expect(screen.queryByRole("heading", { name: "复核队列" })).not.toBeInTheDocument();
  });

  it("filters jobs by keyword and updates the selected detail", () => {
    render(<App />);

    fireEvent.change(screen.getByLabelText("关键词"), { target: { value: "北辰" } });

    expect(screen.getByRole("button", { name: "数据开发实习生" })).toBeInTheDocument();
    expect(screen.queryByRole("button", { name: "前端工程师" })).not.toBeInTheDocument();
    expect(screen.getByText("北辰数据 · 北京")).toBeInTheDocument();
  });

  it("renders the full documented filter surface and filters by city and source trust", () => {
    render(<App />);

    for (const label of [
      "关键词",
      "公司类型",
      "公司",
      "岗位族",
      "岗位类型",
      "城市",
      "技术栈筛选",
      "证书/资质筛选",
      "截止状态",
      "来源可信度",
      "投递状态",
      "复核状态",
      "人工状态"
    ]) {
      expect(screen.getByLabelText(label)).toBeInTheDocument();
    }

    fireEvent.change(screen.getByLabelText("城市"), { target: { value: "深圳" } });
    expect(screen.getByRole("button", { name: "算法工程师" })).toBeInTheDocument();
    expect(screen.queryByRole("button", { name: "前端工程师" })).not.toBeInTheDocument();

    fireEvent.change(screen.getByLabelText("城市"), { target: { value: "" } });
    fireEvent.change(screen.getByLabelText("来源可信度"), { target: { value: "official" } });
    expect(screen.getByRole("button", { name: "前端工程师" })).toBeInTheDocument();
    expect(screen.queryByRole("button", { name: "算法工程师" })).not.toBeInTheDocument();
  });

  it("shows source trust and priority in the job table", () => {
    render(<App />);

    expect(screen.getAllByText("来源可信度").length).toBeGreaterThan(0);
    expect(screen.getAllByText("优先级").length).toBeGreaterThan(0);
    expect(screen.getAllByText("official").length).toBeGreaterThan(0);
    expect(screen.getAllByText("P1").length).toBeGreaterThan(0);
  });

  it("renders source confidence and manual review metadata in job detail", () => {
    render(<App />);

    expect(screen.getByText("来源 云澜校招 · 校招官网")).toBeInTheDocument();
    expect(screen.getByText("来源链接 https://careers.yunlan.example/jobs/frontend")).toBeInTheDocument();
    expect(screen.getByText("置信度 92%")).toBeInTheDocument();
    expect(screen.getByText("人工状态 无需人工")).toBeInTheDocument();
    expect(screen.getByText("登录 否")).toBeInTheDocument();
    expect(screen.getByText("验证码 否")).toBeInTheDocument();
  });

  it("submits manual action records through an injected handler", async () => {
    const onSubmit = vi.fn().mockResolvedValue(undefined);
    render(<ManualActionForm onSubmit={onSubmit} />);

    fireEvent.change(screen.getByLabelText("平台"), { target: { value: "BOSS直聘" } });
    fireEvent.change(screen.getByLabelText("公司名称"), { target: { value: "某公司" } });
    fireEvent.change(screen.getByLabelText("岗位名称"), { target: { value: "后端开发" } });
    fireEvent.change(screen.getByLabelText("搜索关键词"), { target: { value: "后端, Java" } });
    fireEvent.change(screen.getByLabelText("需处理动作"), { target: { value: "manual_check" } });
    fireEvent.change(screen.getByLabelText("处理状态"), { target: { value: "in_progress" } });
    fireEvent.change(screen.getByLabelText("投递方式"), { target: { value: "官网表单" } });
    fireEvent.change(screen.getByLabelText("提醒时间"), { target: { value: "2026-07-01T09:00" } });
    fireEvent.change(screen.getByLabelText("来源链接"), { target: { value: "https://jobs.example/item" } });
    fireEvent.change(screen.getByLabelText("技术栈"), { target: { value: "Java, SQL" } });
    fireEvent.change(screen.getByLabelText("证书/资质"), { target: { value: "英语四级" } });
    fireEvent.change(screen.getByLabelText("优先级"), { target: { value: "2" } });
    fireEvent.click(screen.getByLabelText("需要登录"));
    fireEvent.click(screen.getByLabelText("需要验证码"));
    fireEvent.change(screen.getByLabelText("处理说明"), { target: { value: "先确认官网入口" } });
    fireEvent.click(screen.getByRole("button", { name: "保存处理记录" }));

    await waitFor(() => {
      expect(onSubmit).toHaveBeenCalledWith({
        platform: "BOSS直聘",
        companyName: "某公司",
        jobTitle: "后端开发",
        searchKeywords: ["后端", "Java"],
        requiredAction: "manual_check",
        sourceUrl: "https://jobs.example/item",
        techStackKeywords: ["Java", "SQL"],
        certificateNotes: "英语四级",
        applyMethod: "官网表单",
        reminderAt: "2026-07-01T09:00",
        priority: 2,
        requiresLogin: true,
        requiresCaptcha: true,
        notes: "先确认官网入口",
        status: "in_progress"
      });
    });
  });

  it("records manual submissions in the real App workbench state", async () => {
    window.history.pushState({}, "", "/ops/add");
    render(<App />);

    fireEvent.change(screen.getByLabelText("平台"), { target: { value: "BOSS直聘" } });
    fireEvent.change(screen.getByLabelText("公司名称"), { target: { value: "关键词公司" } });
    fireEvent.change(screen.getByLabelText("搜索关键词"), { target: { value: "秋招, 数据分析" } });
    fireEvent.change(screen.getByLabelText("需处理动作"), { target: { value: "manual_apply" } });
    fireEvent.click(screen.getByRole("button", { name: "保存处理记录" }));

    expect(await screen.findByText("关键词公司 · 秋招, 数据分析")).toBeInTheDocument();
    expect(screen.getByText("manual_apply · pending")).toBeInTheDocument();
  });

  it("does not record manual submissions without a job title or search keywords", async () => {
    window.history.pushState({}, "", "/ops/add");
    render(<App />);

    fireEvent.change(screen.getByLabelText("平台"), { target: { value: "BOSS直聘" } });
    fireEvent.change(screen.getByLabelText("公司名称"), { target: { value: "空岗位公司" } });
    fireEvent.click(screen.getByRole("button", { name: "保存处理记录" }));

    expect(await screen.findByText("请填写岗位名称或搜索关键词。")).toBeInTheDocument();
    expect(screen.queryByText("空岗位公司 · 未命名岗位")).not.toBeInTheDocument();
  });

  it("renders an empty state when no sources are configured", () => {
    render(<SourceList sources={[]} />);

    expect(screen.getByText("暂无岗位来源。")).toBeInTheDocument();
  });
});
