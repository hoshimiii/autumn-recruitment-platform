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

  it("penalizes missing required certificates", () => {
    const matchedCertificateScore = scoreJob({
      sourceTrustLevel: "official",
      techStack: ["Java"],
      certificateRequirements: [{ name: "CET-4", level: "required" }],
      userTechStack: ["Java"],
      userCertificates: ["CET-4"],
      deadlineStatus: "open"
    });

    const missingCertificateScore = scoreJob({
      sourceTrustLevel: "official",
      techStack: ["Java"],
      certificateRequirements: [{ name: "CET-4", level: "required" }],
      userTechStack: ["Java"],
      userCertificates: [],
      deadlineStatus: "open"
    });

    expect(missingCertificateScore).toBeLessThan(matchedCertificateScore - 15);
  });

  it("penalizes closed deadlines", () => {
    const openScore = scoreJob({
      sourceTrustLevel: "official",
      techStack: ["Java"],
      certificateRequirements: [],
      userTechStack: ["Java"],
      userCertificates: [],
      deadlineStatus: "open"
    });

    const closedScore = scoreJob({
      sourceTrustLevel: "official",
      techStack: ["Java"],
      certificateRequirements: [],
      userTechStack: ["Java"],
      userCertificates: [],
      deadlineStatus: "closed"
    });

    expect(closedScore).toBeLessThan(openScore - 25);
    expect(closedScore).toBeLessThan(70);
  });

  it("bounds scores between 0 and 100", () => {
    expect(scoreJob({
      sourceTrustLevel: "official",
      techStack: ["Java"],
      certificateRequirements: [{ name: "CET-4", level: "required" }],
      userTechStack: ["Java"],
      userCertificates: ["CET-4"],
      deadlineStatus: "open"
    })).toBeLessThanOrEqual(100);

    expect(scoreJob({
      sourceTrustLevel: "unknown",
      techStack: ["Java"],
      certificateRequirements: [{ name: "CET-4", level: "required" }],
      userTechStack: [],
      userCertificates: [],
      deadlineStatus: "closed"
    })).toBeGreaterThanOrEqual(0);
  });
});
