import { describe, expect, it } from "vitest";
import { getMicroAppBaseRoute, isMicroAppEnvironment, normalizeAppPath } from "./environment";
import { mount, shouldAutoMount, unmount } from "./lifecycle";

describe("micro-app child runtime", () => {
  it("exposes mount and unmount lifecycle functions", () => {
    expect(mount).toEqual(expect.any(Function));
    expect(unmount).toEqual(expect.any(Function));
  });

  it("detects standalone and embedded micro-app environments", () => {
    expect(shouldAutoMount({})).toBe(true);
    expect(shouldAutoMount({ __MICRO_APP_ENVIRONMENT__: true })).toBe(false);
    expect(isMicroAppEnvironment({ __MICRO_APP_ENVIRONMENT__: true })).toBe(true);
  });

  it("normalizes routes against the micro-app base route", () => {
    expect(getMicroAppBaseRoute({ __MICRO_APP_BASE_ROUTE__: "/recruitment/" })).toBe("/recruitment");
    expect(normalizeAppPath("/recruitment/ops/review", "/recruitment")).toBe("/ops/review");
    expect(normalizeAppPath("/recruitment", "/recruitment")).toBe("/");
  });
});
