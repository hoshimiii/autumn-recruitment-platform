import type { FastifyInstance } from "fastify";

export async function registerReviewItemsRoutes(app: FastifyInstance): Promise<void> {
  app.get("/review-items", async () => {
    return { items: [] };
  });
}
