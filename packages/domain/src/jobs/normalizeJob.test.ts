import { describe, expect, it } from "vitest";
import { normalizeJobCandidate } from "./normalizeJob.js";

describe("normalizeJobCandidate", () => {
  it("maps required display, tech stack, and certificate fields", () => {
    const job = normalizeJobCandidate({
      companyName: " 山东高速集团 ",
      title: " 后端开发工程师 ",
      companyType: "provincial_soe",
      jobType: "campus",
      city: "济南",
      jd: "参与 Java 后端开发，使用 Spring Boot、MySQL、Redis。",
      requirements: "本科及以上，计算机相关专业，英语四级优先。",
      sourceUrl: "https://example.com/job",
      applyUrl: "https://example.com/apply",
      deadline: "2026-09-20"
    });

    expect(job.companyName).toBe("山东高速集团");
    expect(job.title).toBe("后端开发工程师");
    expect(job.techStack).toEqual(["Java", "Spring Boot", "MySQL", "Redis"]);
    expect(job.certificateRequirements).toEqual([
      { name: "CET-4", level: "bonus" }
    ]);
    expect(job.deadline).toBe("2026-09-20");
  });

  it("marks CET-4 as required when required wording is present", () => {
    const job = normalizeJobCandidate({
      companyName: "山东高速集团",
      title: "后端开发工程师",
      companyType: "provincial_soe",
      jobType: "campus",
      city: "济南",
      jd: "参与 Java 后端开发。",
      requirements: "要求通过英语四级，CET-4 required，相关项目经验优先。",
      sourceUrl: "https://example.com/job"
    });

    expect(job.certificateRequirements).toEqual([
      { name: "CET-4", level: "required" }
    ]);
  });
});
