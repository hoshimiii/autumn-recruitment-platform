export type ManualActionStatus = "pending" | "in_progress" | "done" | "ignored";

export type RequiredManualAction =
  | "manual_check"
  | "login_required"
  | "captcha_required"
  | "manual_apply"
  | "verify_official_source";
