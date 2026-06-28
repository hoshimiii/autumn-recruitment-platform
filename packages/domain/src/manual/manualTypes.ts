export const manualActionStatuses = ["pending", "in_progress", "done", "ignored"] as const;

export type ManualActionStatus = (typeof manualActionStatuses)[number];

export const requiredManualActions = [
  "manual_check",
  "login_required",
  "captcha_required",
  "manual_apply",
  "verify_official_source"
] as const;

export type RequiredManualAction = (typeof requiredManualActions)[number];
