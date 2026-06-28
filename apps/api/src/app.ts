import { mkdirSync } from "node:fs";
import { dirname, join } from "node:path";
import {
  createJobRepository,
  createManualItemRepository,
  migrate,
  openDatabase,
  type SqliteDatabase
} from "@recruitment/db";
import Fastify, { type FastifyInstance } from "fastify";
import { ZodError } from "zod";
import { registerJobsRoutes, type JobRepository } from "./routes/jobs.js";
import { registerManualItemsRoutes, type ManualItemRepository } from "./routes/manualItems.js";
import { registerReviewItemsRoutes } from "./routes/reviewItems.js";
import { registerSourcesRoutes } from "./routes/sources.js";
import { registerTasksRoutes } from "./routes/tasks.js";

export interface BuildAppOptions {
  databasePath?: string;
}

export interface RouteContext {
  db: SqliteDatabase;
  jobs: JobRepository;
  manualItems: ManualItemRepository;
}

export async function buildApp(options: BuildAppOptions = {}): Promise<FastifyInstance> {
  const app = Fastify({ logger: false });
  const databasePath = options.databasePath ?? defaultDatabasePath();
  ensureDatabaseDirectory(databasePath);

  const db = openDatabase(databasePath);
  migrate(db);

  app.setErrorHandler((error, _request, reply) => {
    if (error instanceof ZodError) {
      return reply.status(400).send({
        error: {
          code: "validation_error",
          message: "Invalid request payload"
        }
      });
    }

    return reply.send(error);
  });

  const context: RouteContext = {
    db,
    jobs: createJobRepository(db),
    manualItems: createManualItemRepository(db)
  };

  app.addHook("onClose", async () => {
    db.close();
  });

  await app.register(
    async (api) => {
      await registerJobsRoutes(api, context);
      await registerManualItemsRoutes(api, context);
      await registerSourcesRoutes(api);
      await registerTasksRoutes(api);
      await registerReviewItemsRoutes(api);
    },
    { prefix: "/api" }
  );

  return app;
}

function defaultDatabasePath(): string {
  return join(process.cwd(), "data", "recruitment.sqlite");
}

function ensureDatabaseDirectory(databasePath: string): void {
  if (databasePath === ":memory:") {
    return;
  }

  mkdirSync(dirname(databasePath), { recursive: true });
}
