import { defineWorkspace } from "vitest/config";

export default defineWorkspace([
  "packages/domain",
  "packages/db",
  "apps/api",
  "apps/web"
]);
