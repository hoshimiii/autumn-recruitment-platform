# RecruitOps 秋招信息平台

本地优先的秋招信息整理系统。React 微前端负责岗位展示、筛选、详情、来源配置、
采集运行和人工复核；Java 服务负责 SQLite 持久化、合规采集、导出、CLI 与 MCP。

已加入 OIDC SSO 与四级权限、SQLite 并发控制、可选 Redis 缓存与 ES 模糊搜索，
以及两进程浏览器采集到人工待核验的流程。部门、毕业届别、来源行和官方岗位编号
贯穿候选、复核、发布与导出；缺失部门或届别保持未知。

默认仍是仅监听回环地址的本地模式，Redis、ES 和 AI 默认关闭。中间件与 SSO
启动方法见 [infra/README.md](infra/README.md)，浏览器工作进程见
[scripts/browser-workers/README.md](scripts/browser-workers/README.md)。
[Hadoop/HBase/ZooKeeper 实验](labs/analytics/README.md) 使用独立合成数据，
演示批处理与聚合存取；不接管业务数据库。验收证据与未验证项见 [CONTEXT.md](CONTEXT.md)。

## 快速启动

环境要求：Node.js 24、npm 11、Java 17。Maven 由仓库内 Wrapper 提供。

```powershell
npm ci
.\mvnw.cmd -f apps/server/pom.xml spring-boot:run
```

另开一个终端：

```powershell
npm run dev:web
```

打开 `http://127.0.0.1:5173`。后端默认使用相对当前运行目录的
`./data/recruitment.sqlite`，所有示例来源默认停用。

若 Windows 报端口 `EACCES`，先用 `netsh interface ipv4 show excludedportrange protocol=tcp`
确认保留范围；本次验收机器的 8080 和旧测试端口 14300 位于保留范围。可在后端
终端设置 `$env:SERVER_PORT='18189'`，在前端终端设置
`$env:VITE_API_BASE_URL='http://127.0.0.1:18189/api/v1'` 后分别启动，
无需修改系统网络设置。使用 SSO 时同时调整 IdP 客户端的允许回调地址。

## 工作流

1. 在“配置”页新增或编辑公开来源，记录条款 URL 与确认时间，并核对 robots、解析字段和请求频率。
2. 使用“测试”验证提取结果；测试不会写入复核队列。
3. 启用合规来源并运行单源或每日任务。
4. 在“复核”页批准、拒绝或转人工处理。
5. 批准后的岗位出现在首页，可筛选、查看详情并导出 Markdown 或 Excel。

系统不会登录、处理验证码、绕过 robots 或限流，也不会自动投递简历。遇到这些边界时，
请在“添加/导入”页保存人工处理记录。

## 构建与验证

```powershell
.\mvnw.cmd -f apps/server/pom.xml test
.\mvnw.cmd -f apps/server/pom.xml package
npm test
npm run typecheck
npm run build
npm run e2e
npm run smoke:mcp
node --test scripts/browser-workers/*.test.mjs
```

Playwright 使用隔离端口 `15173/18082/24350`，不会复用或停止开发预览，
并避开本地 Sub2API 默认使用的 `18080`。

## CLI

先构建可执行 JAR：

```powershell
.\mvnw.cmd -f apps/server/pom.xml package
$jar = "apps/server/target/recruitment-server-0.1.0-SNAPSHOT.jar"
java -jar $jar cli search --keyword Java
java -jar $jar cli get --id 1
java -jar $jar cli run-source --id 1
java -jar $jar cli export --format md --output output/jobs.md
java -jar $jar cli export --format xlsx --output output/jobs.xlsx
```

CLI、REST 和 MCP 直接调用同一组 Java 应用服务。

## MCP

MCP 使用 STDIO，允许工具固定为：

- `search_jobs`
- `get_job`
- `run_configured_source`
- `export_jobs`

客户端配置示例：

```json
{
  "command": "java",
  "args": [
    "-jar",
    "E:/codex-projects/autumn-recruitment-platform/apps/server/target/recruitment-server-0.1.0-SNAPSHOT.jar",
    "mcp"
  ]
}
```

MCP 不暴露任意 URL、Shell、账号凭据或简历投递能力。

## 微前端接入

生产构建输出稳定入口 `apps/web/dist/assets/recruitment-app.js`，导出 `mount` 和
`unmount`：

```ts
window.__MICRO_APP_ENVIRONMENT__ = true;
window.__MICRO_APP_BASE_ROUTE__ = "/recruitment";
window.__RECRUITMENT_API_BASE_URL__ = "http://127.0.0.1:8080/api/v1";

const app = await import("/assets/recruitment-app.js");
app.mount(document.querySelector("#recruitment-root"));
// app.unmount();
```

子应用路由位于 `/` 和 `/ops/*`，样式限定在 `.recruitment-app` 根节点内。

## 配置

常用环境变量见 [.env.example](.env.example)。在“配置中心”中可设置本地
Sub2API、扫描/处理模型和推理等级；配置页只保存密钥环境变量名，不接收或返回
明文密钥。Agent 默认关闭，只有全局和单个来源同时启用才执行，失败时保留确定性
解析结果进入复核。

本机 Sub2API 默认地址为 `http://127.0.0.1:18080/v1`。启动 Java 服务前在当前
进程环境中设置 `SUB2API_API_KEY`；“检查连接”只读取模型目录，不调用模型。

设计、接口、安全边界和测试证据位于 `docs/`，当前执行状态位于 `CONTEXT.md`。
