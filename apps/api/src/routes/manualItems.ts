import type { createManualItemRepository } from "@recruitment/db";
import { manualActionStatuses, requiredManualActions } from "@recruitment/domain";
import type { FastifyInstance } from "fastify";
import { z } from "zod";
import type { RouteContext } from "../app.js";

export type ManualItemRepository = ReturnType<typeof createManualItemRepository>;

const manualItemInputSchema = z.object({
  platform: z.string().min(1),
  companyName: z.string().min(1),
  jobTitle: z.string().min(1).optional(),
  searchKeywords: z.array(z.string()).optional(),
  sourceUrl: z.string().url().optional(),
  techStackKeywords: z.array(z.string()).optional(),
  certificateNotes: z.string().optional(),
  requiredAction: z.enum(requiredManualActions),
  requiresLogin: z.boolean().optional(),
  requiresCaptcha: z.boolean().optional(),
  applyMethod: z.string().optional(),
  reminderAt: z.string().optional(),
  priority: z.number().int().optional(),
  status: z.enum(manualActionStatuses).optional(),
  linkedJobId: z.number().int().positive().optional(),
  notes: z.string().optional()
}).refine((input) => input.jobTitle || (input.searchKeywords?.length ?? 0) > 0, {
  message: "jobTitle or searchKeywords is required",
  path: ["jobTitle"]
});

export async function registerManualItemsRoutes(app: FastifyInstance, context: RouteContext): Promise<void> {
  app.get("/manual-items", async () => {
    return { items: context.manualItems.list() };
  });

  app.post("/manual-items", async (request, reply) => {
    const input = manualItemInputSchema.parse(request.body);
    const item = context.manualItems.create(input);

    return reply.status(201).send({ item });
  });
}
