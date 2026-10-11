import { render, screen, waitFor } from "@testing-library/react";
import { afterEach, expect, it, vi } from "vitest";
import { App } from "../App";
import { createManualItem } from "../api/client";
import { authUrl, loadSession, logout } from "../api/auth";

afterEach(() => { vi.restoreAllMocks(); delete window.__MICRO_APP_BASE_ROUTE__; delete window.__RECRUITMENT_API_BASE_URL__;
  window.history.replaceState({}, "", "/"); });

it("hides operations and guards a direct route for anonymous OIDC users", async () => {
  window.history.replaceState({}, "", "/ops/config");
  vi.spyOn(globalThis, "fetch").mockResolvedValue(new Response(JSON.stringify({
    mode: "oidc", authenticated: false, roles: [], capabilities: [],
    csrfToken: "masked-token", csrfHeader: "X-XSRF-TOKEN",
    loginUrl: "/oauth2/authorization/recruitops", logoutUrl: "/api/v1/auth/logout"
  })));
  render(<App />);
  expect(await screen.findByText("此操作需要相应权限。" )).toBeInTheDocument();
  expect(screen.queryByRole("link", { name: "配置" })).not.toBeInTheDocument();
  expect(screen.getByRole("link", { name: "登录" })).toHaveAttribute("href", `${window.location.origin}/oauth2/authorization/recruitops`);
});

it("sends session cookies and the server-provided CSRF header on writes", async () => {
  const fetchMock = vi.spyOn(globalThis, "fetch")
    .mockResolvedValueOnce(new Response(JSON.stringify({ mode: "oidc", authenticated: true,
      roles: ["COLLECTOR"], capabilities: ["COLLECTOR"], csrfToken: "masked-token", csrfHeader: "X-XSRF-TOKEN",
      loginUrl: "/oauth2/authorization/recruitops", logoutUrl: "/api/v1/auth/logout" })))
    .mockResolvedValueOnce(new Response(JSON.stringify({ id: 1 })));
  await loadSession(new AbortController().signal);
  await createManualItem({ platform: "fixture", companyName: "Acme", requiredAction: "Review" });
  await waitFor(() => expect(fetchMock).toHaveBeenLastCalledWith("/api/v1/manual-items",
    expect.objectContaining({ credentials: "include", headers: expect.objectContaining({ "X-XSRF-TOKEN": "masked-token" }) })));
});

it("fails closed if the auth response is absent or malformed", async () => {
  vi.spyOn(globalThis, "fetch").mockResolvedValue(new Response("{}"));
  const session = await loadSession(new AbortController().signal);
  expect(session.capabilities).toEqual([]);
});

it("keeps the embedded host route and uses the configured API origin for login", async () => {
  window.__MICRO_APP_BASE_ROUTE__ = "/host/recruitment";
  window.__RECRUITMENT_API_BASE_URL__ = "http://127.0.0.1:8080/api/v1";
  window.history.replaceState({}, "", "/host/recruitment/ops/config");
  vi.spyOn(globalThis, "fetch").mockResolvedValue(new Response(JSON.stringify({
    ...publicOidc, capabilities: ["COLLECTOR"], roles: ["COLLECTOR"], authenticated: true
  })));
  render(<App />);
  expect(await screen.findByText("此操作需要相应权限。")).toBeInTheDocument();
  expect(window.location.pathname).toBe("/host/recruitment/ops/config");
  expect(authUrl(publicOidc.loginUrl)).toBe("http://127.0.0.1:8080/oauth2/authorization/recruitops");
  expect(screen.getByRole("link", { name: "添加/导入" })).toHaveAttribute("href", "/host/recruitment/ops/add");
  expect(screen.queryByRole("link", { name: "复核" })).not.toBeInTheDocument();
});

const publicOidc = { mode: "oidc", authenticated: false, roles: [], capabilities: [],
  csrfToken: "token", csrfHeader: "X-XSRF-TOKEN", loginUrl: "/oauth2/authorization/recruitops", logoutUrl: "/api/v1/auth/logout" };

it("posts logout to the API origin with session credentials and the CSRF token", async () => {
  window.__RECRUITMENT_API_BASE_URL__ = "http://127.0.0.1:8080/api/v1";
  const fetchMock = vi.spyOn(globalThis, "fetch").mockResolvedValue(new Response(null, { status: 204 }));
  await logout({ ...publicOidc, mode: "oidc", authenticated: true });
  expect(fetchMock).toHaveBeenCalledWith("http://127.0.0.1:8080/api/v1/auth/logout", {
    method: "POST", credentials: "include", headers: { "X-XSRF-TOKEN": "token" }
  });
});
