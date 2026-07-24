import { spawn, spawnSync } from "node:child_process";
import { strict as assert } from "node:assert";
import { resolve } from "node:path";

const jar = resolve("apps/server/target/recruitment-server-0.1.0-SNAPSHOT.jar");
const child = spawn("java", ["-jar", jar, "mcp"], {
  env: {
    ...process.env,
    SPRING_DATASOURCE_URL: "jdbc:sqlite:file:recruitops-mcp?mode=memory&cache=shared"
  },
  stdio: ["pipe", "pipe", "pipe"]
});
const expected = ["search_jobs", "get_job", "export_jobs", "run_configured_source"];
let buffer = "";
let finished = false;
let stderr = "";

const send = (message) => child.stdin.write(`${JSON.stringify(message)}\n`);
const stop = (error) => {
  if (finished) return;
  finished = true;
  clearTimeout(timeout);
  if (process.platform === "win32") {
    spawnSync("taskkill", ["/pid", String(child.pid), "/T", "/F"], { stdio: "ignore" });
  } else {
    child.kill();
  }
  if (error) {
    console.error(error instanceof Error ? error.message : error);
    if (stderr) console.error(stderr);
    process.exitCode = 1;
  }
  child.stdin.destroy();
  child.stdout.destroy();
  child.stderr.destroy();
};

child.stderr.on("data", (chunk) => { stderr += chunk; });
child.stdout.on("data", (chunk) => {
  buffer += chunk;
  const lines = buffer.split(/\r?\n/u);
  buffer = lines.pop() ?? "";
  for (const line of lines.filter(Boolean)) {
    try {
      const response = JSON.parse(line);
      if (response.id === 1) {
        send({ jsonrpc: "2.0", method: "notifications/initialized", params: {} });
        send({ jsonrpc: "2.0", id: 2, method: "tools/list", params: {} });
      } else if (response.id === 2) {
        assert.deepEqual(response.result.tools.map((tool) => tool.name), expected);
        send({ jsonrpc: "2.0", id: 3, method: "tools/call",
          params: { name: "search_jobs", arguments: { keyword: "Java" } } });
      } else if (response.id === 3) {
        assert.equal(response.result.isError ?? false, false);
        console.log(`MCP smoke passed: ${expected.join(", ")}`);
        stop();
      }
    } catch (error) {
      stop(error);
    }
  }
});
child.on("error", stop);
child.on("exit", (code) => {
  if (!finished) stop(new Error(`MCP process exited before acceptance (code ${code})`));
});

const timeout = setTimeout(() => stop(new Error("MCP smoke timed out")), 30_000);
send({
  jsonrpc: "2.0",
  id: 1,
  method: "initialize",
  params: {
    protocolVersion: "2024-11-05",
    capabilities: {},
    clientInfo: { name: "recruitops-smoke", version: "1.0" }
  }
});
