package dev.recruitops.mcp;

import io.modelcontextprotocol.json.McpJsonDefaults;
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.McpSyncServer;
import io.modelcontextprotocol.server.transport.StdioServerTransportProvider;
import io.modelcontextprotocol.spec.McpSchema.ServerCapabilities;
import org.springframework.stereotype.Component;

@Component
public class McpRuntime {
  private final McpJobToolRegistry tools;
  private McpSyncServer server;

  public McpRuntime(McpJobToolRegistry tools) { this.tools = tools; }

  public void run() {
    server = McpServer.sync(new StdioServerTransportProvider(McpJsonDefaults.getMapper()))
        .serverInfo("recruitment-platform", "0.1.0")
        .capabilities(ServerCapabilities.builder().tools(false).build())
        .tools(tools.tools())
        .build();
  }
}
