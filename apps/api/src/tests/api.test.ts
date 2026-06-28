import { describe, expect, it } from "vitest";
import { buildApp } from "../app.js";

describe("API", () => {
  it("lists jobs with filter metadata", async () => {
    const app = await buildApp({ databasePath: ":memory:" });
    const response = await app.inject({ method: "GET", url: "/api/jobs?keyword=Java" });

    expect(response.statusCode).toBe(200);
    expect(response.json()).toEqual({
      items: [],
      page: 1,
      pageSize: 20,
      total: 0
    });

    await app.close();
  });

  it("creates manual action records without triggering collection", async () => {
    const app = await buildApp({ databasePath: ":memory:" });
    const response = await app.inject({
      method: "POST",
      url: "/api/manual-items",
      payload: {
        platform: "BOSS直聘",
        companyName: "某公司",
        jobTitle: "后端开发",
        sourceUrl: "https://example.com/search",
        requiredAction: "manual_check",
        requiresLogin: true,
        requiresCaptcha: false,
        status: "pending"
      }
    });

    expect(response.statusCode).toBe(201);
    expect(response.json().item.platform).toBe("BOSS直聘");

    await app.close();
  });

  it("rejects invalid manual action records", async () => {
    const app = await buildApp({ databasePath: ":memory:" });
    const response = await app.inject({
      method: "POST",
      url: "/api/manual-items",
      payload: {
        platform: "",
        companyName: "某公司",
        jobTitle: "后端开发",
        requiredAction: "manual_check"
      }
    });

    expect(response.statusCode).toBe(400);
    expect(response.json()).toEqual({
      error: {
        code: "validation_error",
        message: "Invalid request payload"
      }
    });

    await app.close();
  });
});
