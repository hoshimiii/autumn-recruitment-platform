export type SourceMode = "auto_adapter" | "manual_only";

export type SourceComplianceStatus =
  | "allowed"
  | "login_required"
  | "captcha_required"
  | "forbidden"
  | "unknown";

export type SourcePolicyInput = {
  enabled: boolean;
  mode: SourceMode;
  complianceStatus: SourceComplianceStatus;
};

export type SourcePolicyDecision =
  | { allowed: true }
  | {
      allowed: false;
      reason:
        | "source_disabled"
        | "manual_only"
        | "login_required"
        | "captcha_required"
        | "forbidden"
        | "unknown";
    };

export function canCollectSource(input: SourcePolicyInput): SourcePolicyDecision {
  if (!input.enabled) {
    return { allowed: false, reason: "source_disabled" };
  }

  if (input.mode === "manual_only") {
    return { allowed: false, reason: "manual_only" };
  }

  if (input.complianceStatus !== "allowed") {
    return { allowed: false, reason: input.complianceStatus };
  }

  return { allowed: true };
}
