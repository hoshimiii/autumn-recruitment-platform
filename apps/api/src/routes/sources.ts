import type { FastifyInstance } from "fastify";

export async function registerSourcesRoutes(app: FastifyInstance): Promise<void> {
  app.get("/sources", async () => {
    return { items: [] };
  });
}
