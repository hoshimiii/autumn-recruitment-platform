import { defineConfig, devices } from "@playwright/test";

export default defineConfig({
  testDir: "./tests/e2e",
  timeout: 120_000,
  workers: 1,
  expect: {
    timeout: 10_000
  },
  use: {
    baseURL: "http://127.0.0.1:5173",
    trace: "on-first-retry"
  },
  webServer: [
    {
      command: "node tests/fixtures/career-source.mjs",
      url: "http://127.0.0.1:4300/robots.txt",
      reuseExistingServer: false
    },
    {
      command: ".\\mvnw.cmd -f apps/server/pom.xml spring-boot:run",
      url: "http://127.0.0.1:8080/api/v1/health",
      reuseExistingServer: false,
      timeout: 180_000,
      env: {
        SPRING_DATASOURCE_URL: "jdbc:sqlite:file:recruitops-e2e?mode=memory&cache=shared",
        SPRING_PROFILES_ACTIVE: "test",
        RECRUITOPS_COLLECTOR_ALLOW_PRIVATE_ADDRESSES: "true",
        RECRUITOPS_MINIMUM_FLOOR_SECONDS: "0"
      }
    },
    {
      command: "npm run dev -w apps/web -- --host 127.0.0.1 --port 5173 --strictPort",
      url: "http://127.0.0.1:5173",
      reuseExistingServer: false,
      timeout: 120_000,
      env: { VITE_API_BASE_URL: "http://127.0.0.1:8080/api/v1" }
    }
  ],
  projects: [
    {
      name: "chromium",
      use: { ...devices["Desktop Chrome"], channel: "chrome" }
    }
  ]
});
