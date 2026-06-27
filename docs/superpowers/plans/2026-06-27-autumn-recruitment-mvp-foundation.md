# Autumn Recruitment MVP Foundation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build the first runnable local MVP foundation for the autumn recruitment platform: documented PRD/TRD/QA baseline, TypeScript workspace, SQLite persistence, core domain services, Fastify API, and React workbench UI for job table, detail view, review queue, sources, tasks, and manual action records.

**Architecture:** Use a local-first TypeScript monorepo with clear module boundaries. The frontend calls API routes only; API routes delegate to domain services; domain services depend on repository interfaces; SQLite persistence implements repositories; source collection is represented by policy and task records in this plan, with real source adapters deferred to the next plan.

**Tech Stack:** Node 24, npm workspaces, TypeScript, Vite React, Fastify, better-sqlite3, Zod, Vitest, Testing Library, Playwright.

---

## Scope

This plan implements the MVP foundation only. It does not implement real website crawling, LLM extraction, RSSHub, changedetection.io, or batch resume submission. Those require follow-up plans after the foundation is runnable and tested.

## Agent Operating Rules

- The controller agent owns plan tracking and review decisions.
- Reuse one documentation-maintenance agent when updating PRD/TRD/QA files across tasks.
- Use fresh implementation worker agents for tasks with disjoint file ownership.
- Use research agents only when blocked by a source, library, compliance, or implementation uncertainty that needs current external evidence.
- Close completed agents promptly. Reuse an agent only when its retained context is directly useful for the next related task.
- Use cheaper/faster models for mechanical file generation and stronger models for architecture, research, and review.
- Every implementation task starts with failing tests unless it only changes configuration or documentation.

## File Structure

Create this structure:

```text
apps/
  api/
    package.json
    src/
      app.ts
      index.ts
      routes/
      services/
      tests/
  web/
    package.json
    index.html
    src/
      App.tsx
      main.tsx
      api/
      components/
      features/
      styles/
      tests/
packages/
  domain/
    package.json
    src/
      index.ts
      jobs/
      review/
      sources/
      tasks/
      manual/
  db/
    package.json
    src/
      connection.ts
      migrate.ts
      schema.sql
      repositories/
      tests/
docs/
  README.md
  prd/
  trd/
  qa/
  governance/
```

## Task 1: Workspace And Documentation Baseline

**Files:**
- Create: `package.json`
- Create: `tsconfig.base.json`
- Create: `vitest.workspace.ts`
- Create: `docs/README.md`
- Create: `docs/prd/PRD.md`
- Create: `docs/prd/user-stories.md`
- Create: `docs/prd/acceptance-criteria.md`
- Create: `docs/trd/TRD.md`
- Create: `docs/trd/architecture.md`
- Create: `docs/trd/data-model.md`
- Create: `docs/trd/api-contracts.md`
- Create: `docs/trd/adapter-spec.md`
- Create: `docs/trd/task-flow.md`
- Create: `docs/trd/error-handling.md`
- Create: `docs/trd/compliance-boundary.md`
- Create: `docs/qa/test-plan.md`
- Create: `docs/qa/test-cases.md`
- Create: `docs/qa/tdd-checklist.md`
- Create: `docs/qa/e2e-scenarios.md`
- Create: `docs/governance/risk-register.md`
- Create: `docs/governance/change-log.md`
- Create: `docs/governance/decision-records/ADR-0001-local-sqlite.md`
- Create: `docs/governance/decision-records/ADR-0002-whitelist-adapters.md`

- [ ] **Step 1: Create workspace config**

Create `package.json`:

```json
{
  "name": "autumn-recruitment-platform",
  "private": true,
  "type": "module",
  "workspaces": [
    "apps/*",
    "packages/*"
  ],
  "scripts": {
    "build": "npm run build --workspaces",
    "test": "vitest run --workspace vitest.workspace.ts",
    "test:watch": "vitest --workspace vitest.workspace.ts",
    "lint": "npm run typecheck --workspaces --if-present",
    "typecheck": "npm run typecheck --workspaces --if-present",
    "dev:api": "npm run dev -w apps/api",
    "dev:web": "npm run dev -w apps/web",
    "e2e": "playwright test"
  },
  "devDependencies": {
    "@playwright/test": "^1.61.1",
    "@types/node": "^26.0.1",
    "typescript": "^6.0.3",
    "vitest": "^4.1.9"
  }
}
```

Create `tsconfig.base.json`:

```json
{
  "compilerOptions": {
    "target": "ES2022",
    "module": "NodeNext",
    "moduleResolution": "NodeNext",
    "strict": true,
    "esModuleInterop": true,
    "skipLibCheck": true,
    "forceConsistentCasingInFileNames": true,
    "resolveJsonModule": true
  }
}
```

Create `vitest.workspace.ts`:

```ts
import { defineWorkspace } from "vitest/config";

export default defineWorkspace([
  "packages/domain",
  "packages/db",
  "apps/api",
  "apps/web"
]);
```

- [ ] **Step 2: Create documentation split**

Copy requirements from `docs/superpowers/specs/2026-06-27-autumn-recruitment-platform-design.md` into the PRD/TRD/QA/governance files listed above. Keep each file focused on its documented responsibility.

`docs/README.md` must include:

```md
# Autumn Recruitment Platform Docs

This documentation set tracks the product, technical, testing, and governance records for the local-first autumn recruitment platform.

Start here:

- Product: `prd/PRD.md`
- Technical design: `trd/TRD.md`
- Data model: `trd/data-model.md`
- Test plan: `qa/test-plan.md`
- Risks: `governance/risk-register.md`
- Decisions: `governance/decision-records/`
```

- [ ] **Step 3: Verify documentation has no placeholders**

Run:

```bash
rg -n "T[B]D|T[O]DO|f[i]ll in|implement l[a]ter|待[定]|占[位]" docs package.json tsconfig.base.json vitest.workspace.ts
```

Expected: no matches and exit code 1 from `rg`.

- [ ] **Step 4: Commit**

```bash
git add package.json tsconfig.base.json vitest.workspace.ts docs
git commit -m "docs: split product and technical records"
```

## Task 2: Domain Schemas And Source Policy

**Files:**
- Create: `packages/domain/package.json`
- Create: `packages/domain/tsconfig.json`
- Create: `packages/domain/src/index.ts`
- Create: `packages/domain/src/sources/sourcePolicy.ts`
- Create: `packages/domain/src/sources/sourcePolicy.test.ts`
- Create: `packages/domain/src/jobs/jobTypes.ts`
- Create: `packages/domain/src/manual/manualTypes.ts`

- [ ] **Step 1: Create failing source policy tests**

Create `packages/domain/src/sources/sourcePolicy.test.ts`:

```ts
import { describe, expect, it } from "vitest";
import { canCollectSource } from "./sourcePolicy";

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
```

- [ ] **Step 2: Run test and verify it fails**

Run:

```bash
npm test -- packages/domain/src/sources/sourcePolicy.test.ts
```

Expected: FAIL because `packages/domain` and `canCollectSource` do not exist yet.

- [ ] **Step 3: Implement package and policy**

Create `packages/domain/package.json`:

```json
{
  "name": "@recruitment/domain",
  "private": true,
  "type": "module",
  "main": "src/index.ts",
  "scripts": {
    "build": "tsc -p tsconfig.json",
    "typecheck": "tsc -p tsconfig.json --noEmit",
    "test": "vitest run"
  },
  "dependencies": {
    "zod": "^4.4.3"
  },
  "devDependencies": {
    "vitest": "^4.1.9",
    "typescript": "^6.0.3"
  }
}
```

Create `packages/domain/tsconfig.json`:

```json
{
  "extends": "../../tsconfig.base.json",
  "compilerOptions": {
    "outDir": "dist",
    "rootDir": "src"
  },
  "include": ["src"]
}
```

Create `packages/domain/src/sources/sourcePolicy.ts`:

```ts
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
```

Create `packages/domain/src/jobs/jobTypes.ts` with exported string union types for company type, job type, status, source trust, tech stack tags, and certificate requirement level.

Create `packages/domain/src/manual/manualTypes.ts` with manual action status and required action types.

Create `packages/domain/src/index.ts`:

```ts
export * from "./sources/sourcePolicy";
export * from "./jobs/jobTypes";
export * from "./manual/manualTypes";
```

- [ ] **Step 4: Run test and verify it passes**

Run:

```bash
npm test -- packages/domain/src/sources/sourcePolicy.test.ts
```

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add packages/domain package.json package-lock.json
git commit -m "feat: add domain source policy"
```

## Task 3: Job Normalization, Dedupe, And Scoring

**Files:**
- Create: `packages/domain/src/jobs/normalizeJob.ts`
- Create: `packages/domain/src/jobs/normalizeJob.test.ts`
- Create: `packages/domain/src/jobs/dedupe.ts`
- Create: `packages/domain/src/jobs/dedupe.test.ts`
- Create: `packages/domain/src/jobs/scoreJob.ts`
- Create: `packages/domain/src/jobs/scoreJob.test.ts`
- Modify: `packages/domain/src/index.ts`

- [ ] **Step 1: Write failing normalization test**

Create `packages/domain/src/jobs/normalizeJob.test.ts`:

```ts
import { describe, expect, it } from "vitest";
import { normalizeJobCandidate } from "./normalizeJob";

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
});
```

- [ ] **Step 2: Verify failing test**

Run:

```bash
npm test -w packages/domain -- src/jobs/normalizeJob.test.ts
```

Expected: FAIL because `normalizeJobCandidate` is missing.

- [ ] **Step 3: Implement minimal normalizer**

Create `packages/domain/src/jobs/normalizeJob.ts`:

```ts
export type CertificateRequirementLevel = "required" | "bonus" | "unknown" | "not_mentioned";

export type CertificateRequirement = {
  name: string;
  level: CertificateRequirementLevel;
};

export type JobCandidateInput = {
  companyName: string;
  title: string;
  companyType: string;
  jobType: string;
  city: string;
  jd: string;
  requirements: string;
  sourceUrl: string;
  applyUrl?: string;
  deadline?: string;
};

export type NormalizedJob = JobCandidateInput & {
  techStack: string[];
  certificateRequirements: CertificateRequirement[];
};

const TECH_PATTERNS = ["Java", "Spring Boot", "MySQL", "Redis", "Python", "C++", "React", "Vue", "Linux"];

export function normalizeJobCandidate(input: JobCandidateInput): NormalizedJob {
  const text = `${input.jd}\n${input.requirements}`;

  return {
    ...input,
    companyName: input.companyName.trim(),
    title: input.title.trim(),
    techStack: TECH_PATTERNS.filter((tech) => text.toLowerCase().includes(tech.toLowerCase())),
    certificateRequirements: parseCertificates(text)
  };
}

function parseCertificates(text: string): CertificateRequirement[] {
  const certificates: CertificateRequirement[] = [];

  if (/英语四级|CET-4/i.test(text)) {
    certificates.push({
      name: "CET-4",
      level: /优先|加分/.test(text) ? "bonus" : "unknown"
    });
  }

  return certificates;
}
```

- [ ] **Step 4: Write failing dedupe and scoring tests**

Create `packages/domain/src/jobs/dedupe.test.ts`:

```ts
import { describe, expect, it } from "vitest";
import { buildJobDedupeKey } from "./dedupe";

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
```

Create `packages/domain/src/jobs/scoreJob.test.ts`:

```ts
import { describe, expect, it } from "vitest";
import { scoreJob } from "./scoreJob";

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
```

- [ ] **Step 5: Verify failures, implement, and re-run**

Run:

```bash
npm test -w packages/domain -- src/jobs
```

Expected before implementation: FAIL for missing functions.

Create `packages/domain/src/jobs/dedupe.ts` and `packages/domain/src/jobs/scoreJob.ts` with minimal functions satisfying the tests. Export new functions from `packages/domain/src/index.ts`.

Run again:

```bash
npm test -w packages/domain -- src/jobs
```

Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add packages/domain
git commit -m "feat: add job normalization and scoring"
```

## Task 4: SQLite Persistence And Migrations

**Files:**
- Create: `packages/db/package.json`
- Create: `packages/db/tsconfig.json`
- Create: `packages/db/src/schema.sql`
- Create: `packages/db/src/connection.ts`
- Create: `packages/db/src/migrate.ts`
- Create: `packages/db/src/repositories/jobRepository.ts`
- Create: `packages/db/src/repositories/manualItemRepository.ts`
- Create: `packages/db/src/tests/persistence.test.ts`
- Create: `packages/db/src/index.ts`

- [ ] **Step 1: Write failing persistence test**

Create `packages/db/src/tests/persistence.test.ts`:

```ts
import { mkdtempSync, rmSync } from "node:fs";
import { tmpdir } from "node:os";
import { join } from "node:path";
import { afterEach, describe, expect, it } from "vitest";
import { openDatabase } from "../connection";
import { migrate } from "../migrate";
import { createJobRepository } from "../repositories/jobRepository";
import { createManualItemRepository } from "../repositories/manualItemRepository";

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
```

- [ ] **Step 2: Verify failing test**

Run:

```bash
npm test -w packages/db -- src/tests/persistence.test.ts
```

Expected: FAIL because `packages/db` does not exist.

- [ ] **Step 3: Implement SQLite package**

Create `packages/db/package.json` with dependencies:

```json
{
  "name": "@recruitment/db",
  "private": true,
  "type": "module",
  "main": "src/index.ts",
  "scripts": {
    "build": "tsc -p tsconfig.json",
    "typecheck": "tsc -p tsconfig.json --noEmit",
    "test": "vitest run"
  },
  "dependencies": {
    "better-sqlite3": "^12.11.1"
  },
  "devDependencies": {
    "@types/better-sqlite3": "^7.6.13",
    "typescript": "^6.0.3",
    "vitest": "^4.1.9"
  }
}
```

Create schema tables for `sources`, `crawl_tasks`, `review_items`, `jobs`, `job_snapshots`, `manual_items`, and `audit_logs` in `packages/db/src/schema.sql`. Include JSON text columns for arrays such as `tech_stack` and `certificate_requirements`.

Create connection, migration, and repository modules. Repositories should hide SQL details from API/domain callers.

- [ ] **Step 4: Run persistence test**

Run:

```bash
npm test -w packages/db -- src/tests/persistence.test.ts
```

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add packages/db package.json package-lock.json
git commit -m "feat: add sqlite persistence"
```

## Task 5: Fastify API Foundation

**Files:**
- Create: `apps/api/package.json`
- Create: `apps/api/tsconfig.json`
- Create: `apps/api/src/app.ts`
- Create: `apps/api/src/index.ts`
- Create: `apps/api/src/routes/jobs.ts`
- Create: `apps/api/src/routes/manualItems.ts`
- Create: `apps/api/src/routes/sources.ts`
- Create: `apps/api/src/routes/tasks.ts`
- Create: `apps/api/src/routes/reviewItems.ts`
- Create: `apps/api/src/tests/api.test.ts`

- [ ] **Step 1: Write failing API tests**

Create `apps/api/src/tests/api.test.ts`:

```ts
import { describe, expect, it } from "vitest";
import { buildApp } from "../app";

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
  });
});
```

- [ ] **Step 2: Verify failing tests**

Run:

```bash
npm test -w apps/api -- src/tests/api.test.ts
```

Expected: FAIL because API app does not exist.

- [ ] **Step 3: Implement Fastify app and routes**

Create `apps/api/package.json`:

```json
{
  "name": "@recruitment/api",
  "private": true,
  "type": "module",
  "scripts": {
    "dev": "tsx watch src/index.ts",
    "build": "tsc -p tsconfig.json",
    "typecheck": "tsc -p tsconfig.json --noEmit",
    "test": "vitest run"
  },
  "dependencies": {
    "@recruitment/db": "*",
    "@recruitment/domain": "*",
    "fastify": "^5.8.5",
    "zod": "^4.4.3"
  },
  "devDependencies": {
    "tsx": "^4.22.4",
    "typescript": "^6.0.3",
    "vitest": "^4.1.9"
  }
}
```

`buildApp` should create an in-memory or file-backed SQLite connection, run migrations, register routes, and return the Fastify instance.

Required endpoints for this task:

- `GET /api/jobs`
- `GET /api/jobs/:id`
- `POST /api/manual-items`
- `GET /api/manual-items`
- `GET /api/sources`
- `GET /api/tasks`
- `GET /api/review-items`

Mutation endpoints beyond manual item creation may return `501` with a typed JSON body until their domain services are implemented in a later task.

- [ ] **Step 4: Run API tests**

Run:

```bash
npm test -w apps/api -- src/tests/api.test.ts
```

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add apps/api packages/db packages/domain package.json package-lock.json
git commit -m "feat: add api foundation"
```

## Task 6: React Workbench UI

**Files:**
- Create: `apps/web/package.json`
- Create: `apps/web/tsconfig.json`
- Create: `apps/web/vitest.config.ts`
- Create: `apps/web/index.html`
- Create: `apps/web/src/main.tsx`
- Create: `apps/web/src/App.tsx`
- Create: `apps/web/src/api/client.ts`
- Create: `apps/web/src/features/jobs/JobTable.tsx`
- Create: `apps/web/src/features/jobs/JobDetail.tsx`
- Create: `apps/web/src/features/manual/ManualActionForm.tsx`
- Create: `apps/web/src/features/review/ReviewQueue.tsx`
- Create: `apps/web/src/features/sources/SourceList.tsx`
- Create: `apps/web/src/features/tasks/TaskList.tsx`
- Create: `apps/web/src/styles/app.css`
- Create: `apps/web/src/tests/App.test.tsx`
- Create: `apps/web/src/tests/setup.ts`

- [ ] **Step 1: Write failing UI tests**

Create `apps/web/src/tests/App.test.tsx`:

```tsx
import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { App } from "../App";

describe("App", () => {
  it("renders the job workbench with tech stack and certificate columns", () => {
    render(<App />);

    expect(screen.getByRole("heading", { name: "岗位工作台" })).toBeInTheDocument();
    expect(screen.getByText("技术栈")).toBeInTheDocument();
    expect(screen.getByText("证书/资质")).toBeInTheDocument();
  });

  it("renders the manual action form for login and captcha sources", () => {
    render(<App />);

    expect(screen.getByRole("heading", { name: "人工处理" })).toBeInTheDocument();
    expect(screen.getByLabelText("需要登录")).toBeInTheDocument();
    expect(screen.getByLabelText("需要验证码")).toBeInTheDocument();
  });
});
```

- [ ] **Step 2: Verify failing tests**

Run:

```bash
npm test -w apps/web -- src/tests/App.test.tsx
```

Expected: FAIL because web app does not exist.

- [ ] **Step 3: Implement Vite React app**

Create `apps/web/package.json`:

```json
{
  "name": "@recruitment/web",
  "private": true,
  "type": "module",
  "scripts": {
    "dev": "vite --host 127.0.0.1",
    "build": "tsc -p tsconfig.json && vite build",
    "typecheck": "tsc -p tsconfig.json --noEmit",
    "test": "vitest run"
  },
  "dependencies": {
    "@vitejs/plugin-react": "^6.0.3",
    "lucide-react": "^1.21.0",
    "vite": "^8.1.0",
    "react": "^19.2.7",
    "react-dom": "^19.2.7"
  },
  "devDependencies": {
    "@testing-library/jest-dom": "^6.9.1",
    "@testing-library/react": "^16.3.2",
    "@types/react": "^19.2.17",
    "@types/react-dom": "^19.2.3",
    "jsdom": "^29.1.1",
    "typescript": "^6.0.3",
    "vitest": "^4.1.9"
  }
}
```

Create `apps/web/vitest.config.ts`:

```ts
import react from "@vitejs/plugin-react";
import { defineConfig } from "vitest/config";

export default defineConfig({
  plugins: [react()],
  test: {
    environment: "jsdom",
    setupFiles: ["src/tests/setup.ts"]
  }
});
```

Create `apps/web/src/tests/setup.ts`:

```ts
import "@testing-library/jest-dom/vitest";
```

Create a workbench-first UI. It should have:

- Left navigation with Jobs, Review, Sources, Tasks, Manual.
- Summary band with counts.
- Job table with type, company, title, job type, city, tech stack, certificates, deadline, status.
- Detail panel with JD, requirements, tech stack, certificates, source snapshots, and application info.
- Manual action form with login/captcha fields.

Use plain CSS modules or a single `app.css` for the first version. Keep component files focused and under 300 lines.

- [ ] **Step 4: Run UI tests**

Run:

```bash
npm test -w apps/web -- src/tests/App.test.tsx
```

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add apps/web package.json package-lock.json
git commit -m "feat: add recruitment workbench ui"
```

## Task 7: End-To-End Verification And Dev Run

**Files:**
- Create: `playwright.config.ts`
- Create: `tests/e2e/workbench.spec.ts`
- Modify: `package.json`
- Modify: `docs/governance/change-log.md`

- [ ] **Step 1: Write E2E test**

Create `tests/e2e/workbench.spec.ts`:

```ts
import { expect, test } from "@playwright/test";

test("workbench shows jobs, detail fields, and manual action form", async ({ page }) => {
  await page.goto("/");

  await expect(page.getByRole("heading", { name: "岗位工作台" })).toBeVisible();
  await expect(page.getByText("技术栈")).toBeVisible();
  await expect(page.getByText("证书/资质")).toBeVisible();
  await expect(page.getByRole("heading", { name: "人工处理" })).toBeVisible();
  await expect(page.getByLabel("需要登录")).toBeVisible();
  await expect(page.getByLabel("需要验证码")).toBeVisible();
});
```

- [ ] **Step 2: Verify E2E fails before app is running**

Run:

```bash
npm run e2e
```

Expected before Playwright config: FAIL because config/server is missing.

- [ ] **Step 3: Add Playwright config**

Create `playwright.config.ts`:

```ts
import { defineConfig } from "@playwright/test";

export default defineConfig({
  testDir: "tests/e2e",
  webServer: {
    command: "npm run dev -w apps/web",
    url: "http://127.0.0.1:5173",
    reuseExistingServer: true,
    timeout: 120000
  },
  use: {
    baseURL: "http://127.0.0.1:5173",
    trace: "on-first-retry"
  }
});
```

- [ ] **Step 4: Run full verification**

Run:

```bash
npm install
npm run lint
npm test
npm run build
npm run e2e
```

Expected: all commands exit 0.

- [ ] **Step 5: Update change log**

Append to `docs/governance/change-log.md`:

```md
## 2026-06-27

- Added runnable MVP foundation plan implementation.
- Added local TypeScript workspace.
- Added SQLite persistence for jobs and manual action records.
- Added Fastify API foundation.
- Added React workbench UI with tech stack and certificate fields.
- Verified with lint, unit tests, build, and E2E tests.
```

- [ ] **Step 6: Commit**

```bash
git add package.json package-lock.json playwright.config.ts tests apps packages docs/governance/change-log.md
git commit -m "test: verify mvp foundation"
```

## Completion Criteria

- `npm run lint` passes.
- `npm test` passes.
- `npm run build` passes.
- `npm run e2e` passes.
- Job table displays tech stack and certificate columns.
- Job detail displays JD, requirements, tech stack, certificate/qualification requirements, source, and application fields.
- Manual action form records login/captcha/manual-only sources.
- SQLite persists records after reopening.
- No source adapter attempts to collect login-required, captcha-required, forbidden, unknown, disabled, or manual-only sources.
- All implementation changes are documented in PRD/TRD/QA/governance files.

## Follow-Up Plans

Create separate plans after this foundation:

- Real source adapters: GovNotice, AttachmentExtractor, PublicATS, AggregatorIndex.
- Scheduler and changedetection/RSS integration.
- Review merge workflow and application timeline.
- LLM-assisted extraction with strict review gate.
- Resume/application preparation and future batch submission design.
