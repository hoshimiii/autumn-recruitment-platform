import type { createJobRepository } from "@recruitment/db";
import type { FastifyInstance } from "fastify";
import { z } from "zod";
import type { RouteContext } from "../app.js";

export type JobRepository = ReturnType<typeof createJobRepository>;

const listJobsQuerySchema = z.object({
  keyword: z.string().optional()
});

export async function registerJobsRoutes(app: FastifyInstance, context: RouteContext): Promise<void> {
  app.get("/jobs", async (request) => {
    const query = listJobsQuerySchema.parse(request.query);
    const items = context.jobs.list({ keyword: query.keyword });

    return {
      items,
      page: 1,
      pageSize: 20,
      total: items.length
    };
  });

  app.get("/jobs/:id", async (_request, reply) => {
    return reply.status(404).send({
      error: {
        code: "not_found",
        message: "Job not found"
      }
    });
  });
}
