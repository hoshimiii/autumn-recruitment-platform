import { getApiBaseUrl, setCsrfToken } from "./client";

export type Capability = "VIEWER" | "COLLECTOR" | "REVIEWER" | "ADMIN";
export type AuthSession = {
  mode: "local" | "oidc"; authenticated: boolean; roles: Capability[]; capabilities: Capability[];
  csrfToken: string; csrfHeader: string; loginUrl: string; logoutUrl: string;
};
export const publicSession: AuthSession = { mode: "oidc", authenticated: false, roles: [], capabilities: [],
  csrfToken: "", csrfHeader: "", loginUrl: "", logoutUrl: "" };

export async function loadSession(signal: AbortSignal): Promise<AuthSession> {
  setCsrfToken("", "");
  const response = await fetch(`${getApiBaseUrl()}/auth/session`, { credentials: "include", signal,
    headers: { Accept: "application/json" } });
  if (!response.ok) throw new Error("无法加载登录状态。");
  const value = await response.json() as Partial<AuthSession>;
  if ((value.mode !== "local" && value.mode !== "oidc") || !Array.isArray(value.capabilities)
      || !Array.isArray(value.roles) || typeof value.authenticated !== "boolean") return publicSession;
  const session = { ...publicSession, ...value } as AuthSession;
  setCsrfToken(session.csrfHeader, session.csrfToken);
  return session;
}

export async function logout(session: AuthSession): Promise<void> {
  const response = await fetch(authUrl(session.logoutUrl), { method: "POST", credentials: "include",
    headers: { [session.csrfHeader]: session.csrfToken } });
  if (!response.ok) throw new Error("退出失败，请重试。");
}

export function authUrl(path: string): string {
  return new URL(path, new URL(getApiBaseUrl(), window.location.href)).href;
}
