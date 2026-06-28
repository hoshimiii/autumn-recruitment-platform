import { describe, expect, it } from "vitest";
import { scoreJob } from "./scoreJob.js";

describe("scoreJob", () => {
  it("rewards trusted sources, matching tech stack, and certificate fit", () => {
    const score = scoreJob({
      sourceTrustLevel: "official",
      techStack: ["Java", "MySQL"],
      certificateRequirements: [{ name: "CET-4", level: "bonus" }],
      userTechStack: ["Java"],
      userCertificates: ["CET-4"],
      deadlineStatus: "open"
    });

    expect(score).toBeGreaterThan(70);
  });
});
