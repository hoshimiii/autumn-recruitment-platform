import { mkdtempSync, rmSync } from "node:fs";
import { tmpdir } from "node:os";
import { join } from "node:path";
import { afterEach, describe, expect, it } from "vitest";
import { openDatabase } from "../connection.js";
import { migrate } from "../migrate.js";
import { createJobRepository } from "../repositories/jobRepository.js";
import { createManualItemRepository } from "../repositories/manualItemRepository.js";

let tempDir = "";

afterEach(() => {
  if (tempDir) rmSync(tempDir, { recursive: true, force: true });
});

describe("SQLite persistence", () => {
  it("persists jobs and manual items after reopening the database", () => {
    tempDir = mkdtempSync(join(tmpdir(), "recruitment-db-"));
    const dbPath = join(tempDir, "app.sqlite");

    const firstDb = openDatabase(dbPath);
    migrate(firstDb);
    createJobRepository(firstDb).create({
      companyName: "腾讯",
      title: "后台开发",
      city: "深圳",
      jobType: "campus",
      techStack: ["Java"],
      certificateRequirements: [],
      status: "tracking"
    });
    createManualItemRepository(firstDb).create({
      platform: "BOSS直聘",
      companyName: "某公司",
      jobTitle: "Java开发",
      sourceUrl: "https://example.com/search",
      requiredAction: "manual_check",
      requiresLogin: true,
      requiresCaptcha: false,
      status: "pending"
    });
    firstDb.close();

    const secondDb = openDatabase(dbPath);
    migrate(secondDb);

    expect(createJobRepository(secondDb).list({ keyword: "腾讯" })).toHaveLength(1);
    expect(createManualItemRepository(secondDb).list()).toHaveLength(1);
    secondDb.close();
  });
});
