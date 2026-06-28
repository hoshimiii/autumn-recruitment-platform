import { pathToFileURL } from "node:url";
import { buildApp } from "./app.js";

if (import.meta.url === pathToFileURL(process.argv[1] ?? "").href) {
  const app = await buildApp();
  const port = Number(process.env.PORT ?? 4000);

  await app.listen({ port, host: "0.0.0.0" });
}
