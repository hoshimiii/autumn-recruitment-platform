import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { App } from "../App";

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
});
