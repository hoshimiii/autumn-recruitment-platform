import type { FastifyInstance } from "fastify";

export async function registerTasksRoutes(app: FastifyInstance): Promise<void> {
  app.get("/tasks", async () => {
    return { items: [] };
  });
}
