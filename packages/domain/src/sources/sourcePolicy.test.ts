import { describe, expect, it } from "vitest";
import { canCollectSource } from "./sourcePolicy.js";

describe("canCollectSource", () => {
  it("allows enabled allowed auto adapter sources", () => {
    expect(canCollectSource({ enabled: true, mode: "auto_adapter", complianceStatus: "allowed" })).toEqual({
      allowed: true
    });
  });

  it("skips disabled sources", () => {
    expect(canCollectSource({ enabled: false, mode: "auto_adapter", complianceStatus: "allowed" })).toEqual({
      allowed: false,
      reason: "source_disabled"
    });
  });

  it("blocks manual only sources", () => {
    expect(canCollectSource({ enabled: true, mode: "manual_only", complianceStatus: "allowed" })).toEqual({
      allowed: false,
      reason: "manual_only"
    });
  });

  it("blocks login, captcha, forbidden, and unknown compliance states", () => {
    for (const complianceStatus of ["login_required", "captcha_required", "forbidden", "unknown"] as const) {
      expect(canCollectSource({ enabled: true, mode: "auto_adapter", complianceStatus })).toEqual({
        allowed: false,
        reason: complianceStatus
      });
    }
  });
});
