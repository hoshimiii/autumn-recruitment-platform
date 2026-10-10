export type ApiErrorBody = { error?: { code?: string; message?: string; details?: Record<string, unknown> } };

export class ApiError extends Error {
  constructor(public readonly status: number, message: string) { super(message); }
}

export type EntityId = number | string;

export type Job = {
  id: EntityId;
  companyCategory: string;
  companyName: string;
  title: string;
  jobFamily: string;
  jobType: string;
  city: string;
  techStack: string[];
  certificateRequirements: string[];
  deadline?: string;
  deadlineStatus?: string;
  status: string;
  priority: number;
  sourceName: string;
  sourceType: string;
  sourceUrl: string;
  sourceTrust: string;
  capturedAt?: string;
  updatedAt?: string;
  reviewStatus: string;
  applicationStatus?: string;
  jd?: string;
  requirements?: string;
  applyUrl?: string;
  applyMethod?: string;
  requiresLogin?: boolean;
  requiresCaptcha?: boolean;
  confidence?: number;
  snapshots?: JobSnapshot[];
};
export type JobSnapshot = { id: EntityId; sourceId?: EntityId; sourceUrl: string; contentHash?: string;
  parsedPayload?: Record<string, unknown>; capturedAt: string };

export type JobFilters = {
  keyword: string; companyCategory: string; company: string; jobType: string; city: string;
  techStack: string; certificate: string; deadlineFrom: string; deadlineTo: string; sourceTrust: string;
};

export const emptyJobFilters: JobFilters = {
  keyword: "", companyCategory: "", company: "", jobType: "", city: "", techStack: "", certificate: "",
  deadlineFrom: "", deadlineTo: "", sourceTrust: ""
};

export type JobPage = { items: Job[]; page: number; pageSize: number; total: number };
export type Source = {
  id: EntityId; name: string; type: string; baseUrl: string; adapterType: string; mode: "JSON" | "HTML" | "MANUAL";
  listUrl: string; trustLevel: string; companyCategory: string; enabled: boolean; cron: string; timezone: string;
  minIntervalSeconds: number; maxPages: number; maxItems: number; selectors?: string; fieldMapping?: string;
  robotsUrl?: string; termsUrl?: string; termsConfirmedAt?: string; timeoutSeconds: number; etag?: string;
  lastModified?: string; complianceStatus: string; lastRunAt?: string; lastStatus?: string; aiEnabled: boolean;
  aiProvider?: string; aiModel?: string; configVersion: number;
};

export type SourceInput = Omit<Source, "id" | "etag" | "lastModified" | "lastRunAt" | "lastStatus" | "configVersion"> & { secretEnvRef?: string };
export type ReasoningEffort = "low" | "medium" | "high" | "xhigh";
export type AgentSettings = {
  enabled: boolean; baseUrl: string; secretEnvRef: string; secretConfigured: boolean; scanModel: string;
  scanReasoningEffort: ReasoningEffort; processModel: string; processReasoningEffort: ReasoningEffort;
  timeoutSeconds: number; configVersion: number; updatedAt?: string;
};
export type AgentSettingsInput = Pick<AgentSettings, "enabled" | "baseUrl" | "secretEnvRef" | "scanModel" | "scanReasoningEffort" | "processModel" | "processReasoningEffort" | "timeoutSeconds">;
export type AgentSettingsCheck = { connected: boolean; secretConfigured: boolean; models: string[]; scanModelAvailable: boolean; processModelAvailable: boolean; message: string };
export type ReviewItem = { id: EntityId; companyName: string; title: string; reason?: string; confidence?: number; status: string; sourceName?: string; createdAt?: string };
export type ReviewDetail = { id: EntityId; sourceId?: EntityId; sourceName?: string; status: string;
  candidatePayload: Record<string, unknown>; normalizedPayload: Record<string, unknown>; reason?: string;
  confidence?: number; sourceUrl?: string; dedupeKey?: string; contentHash?: string;
  duplicateJobId?: EntityId; createdAt?: string };
export type Run = { id: EntityId; sourceName?: string; triggerType: string; status: string; startedAt?: string; finishedAt?: string; discoveredCount: number; reviewItemCount: number; errorCount: number; errorSummary?: string };
export type ManualActionInput = { platform: string; companyName: string; jobTitle?: string; searchKeywords?: string[]; sourceUrl?: string; techStackKeywords?: string[]; certificateNotes?: string; requiredAction: string; requiresLogin?: boolean; requiresCaptcha?: boolean; applyMethod?: string; reminderAt?: string; priority?: number; status?: string; notes?: string };
export type ManualItem = ManualActionInput & { id: EntityId; createdAt?: string };

type HostConfig = Window & { __RECRUITMENT_API_BASE_URL__?: string };

export function getApiBaseUrl(target: HostConfig = window): string {
  const configured = target.__RECRUITMENT_API_BASE_URL__ ?? import.meta.env.VITE_API_BASE_URL ?? "/api/v1";
  return configured.replace(/\/$/u, "");
}

function url(path: string): string { return `${getApiBaseUrl()}${path}`; }

async function request<T>(path: string, init: RequestInit = {}): Promise<T> {
  const response = await fetch(url(path), { ...init, headers: { Accept: "application/json", ...init.headers } });
  if (!response.ok) {
    const body = await response.json().catch(() => ({})) as ApiErrorBody;
    throw new ApiError(response.status, body.error?.message ?? `请求失败 (${response.status})`);
  }
  return response.status === 204 ? undefined as T : response.json() as Promise<T>;
}

function list<T>(value: T[] | { items: T[] }): T[] { return Array.isArray(value) ? value : value.items; }

export function listJobs(filters: JobFilters, signal: AbortSignal): Promise<JobPage> {
  const query = new URLSearchParams({ page: "1", pageSize: "50", sort: "updatedAt,desc" });
  for (const [key, value] of Object.entries(filters)) if (value) query.set(key, value);
  return request<JobPage>(`/jobs?${query}`, { signal });
}

export function getJob(id: EntityId, signal: AbortSignal): Promise<Job> { return request<Job>(`/jobs/${encodeURIComponent(String(id))}`, { signal }); }
export function listSources(signal: AbortSignal): Promise<Source[]> { return request<Source[] | { items: Source[] }>("/sources", { signal }).then(list); }
export function createSource(input: SourceInput): Promise<Source> { return request<Source>("/sources", { method: "POST", headers: { "Content-Type": "application/json" }, body: JSON.stringify(input) }); }
export function updateSource(id: EntityId, input: SourceInput): Promise<Source> { return request<Source>(`/sources/${encodeURIComponent(String(id))}`, { method: "PUT", headers: { "Content-Type": "application/json" }, body: JSON.stringify(input) }); }
export function deleteSource(id: EntityId): Promise<void> { return request<void>(`/sources/${encodeURIComponent(String(id))}`, { method: "DELETE" }); }
export function getAgentSettings(signal: AbortSignal): Promise<AgentSettings> { return request<AgentSettings>("/agent-settings", { signal }); }
export function updateAgentSettings(input: AgentSettingsInput): Promise<AgentSettings> { return request<AgentSettings>("/agent-settings", { method: "PUT", headers: { "Content-Type": "application/json" }, body: JSON.stringify(input) }); }
export function checkAgentSettings(): Promise<AgentSettingsCheck> { return request<AgentSettingsCheck>("/agent-settings/check", { method: "POST" }); }
export function testSource(id: EntityId): Promise<Run> { return request<Run>(`/sources/${encodeURIComponent(String(id))}/test`, { method: "POST" }); }
export function runSource(id: EntityId): Promise<Run> { return request<Run>(`/sources/${encodeURIComponent(String(id))}/runs`, { method: "POST" }); }
export function listReviewItems(signal: AbortSignal): Promise<ReviewItem[]> { return request<ReviewItem[] | { items: ReviewItem[] }>("/review-items", { signal }).then(list); }
export function getReviewItem(id: EntityId): Promise<ReviewDetail> {
  return request<ReviewDetail>(`/review-items/${encodeURIComponent(String(id))}`);
}
export function reviewAction(id: EntityId, action: "approve" | "reject" | "block", body: Record<string, unknown> = {}): Promise<void> { return request<void>(`/review-items/${encodeURIComponent(String(id))}/${action}`, { method: "POST", headers: { "Content-Type": "application/json" }, body: JSON.stringify(body) }); }
export function listRuns(signal: AbortSignal): Promise<Run[]> { return request<Run[] | { items: Run[] }>("/runs", { signal }).then(list); }
export function startDailyRun(): Promise<Run> { return request<Run>("/runs/daily", { method: "POST" }); }
export function listManualItems(signal: AbortSignal): Promise<ManualItem[]> { return request<ManualItem[] | { items: ManualItem[] }>("/manual-items", { signal }).then(list); }
export function createManualItem(input: ManualActionInput): Promise<ManualItem> { return request<ManualItem>("/manual-items", { method: "POST", headers: { "Content-Type": "application/json" }, body: JSON.stringify(input) }); }
export function exportHref(format: "md" | "xlsx", filters: JobFilters): string { const query = new URLSearchParams(); for (const [key, value] of Object.entries(filters)) if (value) query.set(key, value); return `${url(`/exports/jobs.${format}`)}${query.size ? `?${query}` : ""}`; }
export function readJobFilters(search = window.location.search): JobFilters { const params = new URLSearchParams(search); return Object.fromEntries(Object.keys(emptyJobFilters).map((key) => [key, params.get(key) ?? ""])) as JobFilters; }
export function writeJobFilters(filters: JobFilters): void { const query = new URLSearchParams(); for (const [key, value] of Object.entries(filters)) if (value) query.set(key, value); window.history.replaceState({}, "", `${window.location.pathname}${query.size ? `?${query}` : ""}`); }
