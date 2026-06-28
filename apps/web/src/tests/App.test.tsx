import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import { App } from "../App";
import { ManualActionForm } from "../features/manual/ManualActionForm";
import { SourceList } from "../features/sources/SourceList";

describe("App", () => {
  it("renders the job workbench with tech stack and certificate columns", () => {
    render(<App />);

    expect(screen.getByRole("heading", { name: "岗位工作台" })).toBeInTheDocument();
    expect(screen.getByText("技术栈")).toBeInTheDocument();
    expect(screen.getByText("证书/资质")).toBeInTheDocument();
  });

  it("renders the manual action form for login and captcha sources", () => {
    render(<App />);

    expect(screen.getByRole("heading", { name: "人工处理" })).toBeInTheDocument();
    expect(screen.getByLabelText("需要登录")).toBeInTheDocument();
    expect(screen.getByLabelText("需要验证码")).toBeInTheDocument();
  });

  it("filters jobs by keyword and updates the selected detail", () => {
    render(<App />);

    fireEvent.change(screen.getByLabelText("关键词"), { target: { value: "北辰" } });

    expect(screen.getByRole("button", { name: "数据开发实习生" })).toBeInTheDocument();
    expect(screen.queryByRole("button", { name: "前端工程师" })).not.toBeInTheDocument();
    expect(screen.getByText("北辰数据 · 北京")).toBeInTheDocument();
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
    fireEvent.change(screen.getByLabelText("需处理动作"), { target: { value: "manual_check" } });
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
        requiredAction: "manual_check",
        sourceUrl: "https://jobs.example/item",
        techStackKeywords: ["Java", "SQL"],
        certificateNotes: "英语四级",
        priority: 2,
        requiresLogin: true,
        requiresCaptcha: true,
        notes: "先确认官网入口",
        status: "pending"
      });
    });
  });

  it("renders an empty state when no sources are configured", () => {
    render(<SourceList sources={[]} />);

    expect(screen.getByText("暂无岗位来源。")).toBeInTheDocument();
  });
});
