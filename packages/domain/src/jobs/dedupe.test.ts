import { describe, expect, it } from "vitest";
import { buildJobDedupeKey } from "./dedupe.js";

describe("buildJobDedupeKey", () => {
  it("uses normalized company, title, city, job type, and deadline", () => {
    expect(buildJobDedupeKey({
      companyName: " 山东高速集团 ",
      title: "后端开发工程师",
      city: "济南",
      jobType: "campus",
      deadline: "2026-09-20"
    })).toBe("山东高速集团|后端开发工程师|济南|campus|2026-09-20");
  });
});
