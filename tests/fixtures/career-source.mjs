import { createServer } from "node:http";

const job = {
  company: "齐鲁数科",
  title: "Java 开发工程师",
  city: "济南",
  jobFamily: "开发",
  jobType: "全职",
  techStack: ["Java", "Spring Boot", "SQLite"],
  certificates: ["英语四级"],
  deadline: "2026-11-30",
  jd: "参与招聘信息平台后端研发与数据治理。",
  requirements: "熟悉 Java 与 Spring Boot，具备数据库基础。",
  url: "http://127.0.0.1:4300/jobs/java-platform",
  applyUrl: "http://127.0.0.1:4300/apply/java-platform"
};

createServer((request, response) => {
  if (request.url === "/robots.txt") {
    response.writeHead(200, { "Content-Type": "text/plain; charset=utf-8" });
    response.end("User-agent: *\nAllow: /\n");
    return;
  }
  if (request.url?.startsWith("/jobs")) {
    response.writeHead(200, { "Content-Type": "application/json; charset=utf-8" });
    response.end(JSON.stringify({ items: [job] }));
    return;
  }
  response.writeHead(404).end();
}).listen(4300, "127.0.0.1");
