import { existsSync, readFileSync } from "node:fs";
import { dirname, join } from "node:path";
import { fileURLToPath } from "node:url";
import type { SqliteDatabase } from "./connection.js";

const moduleDir = dirname(fileURLToPath(import.meta.url));
const schemaPathCandidates = [join(moduleDir, "schema.sql"), join(moduleDir, "..", "src", "schema.sql")];

export function migrate(db: SqliteDatabase): void {
  const schemaPath = schemaPathCandidates.find((candidate) => existsSync(candidate));
  if (!schemaPath) {
    throw new Error(`Unable to locate schema.sql from ${moduleDir}`);
  }

  db.exec(readFileSync(schemaPath, "utf8"));
}
