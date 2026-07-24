package dev.recruitops.mcp;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.recruitops.application.JobExportService;
import dev.recruitops.application.JobService;
import dev.recruitops.infrastructure.JobRepository;
import io.modelcontextprotocol.server.McpServerFeatures.SyncToolSpecification;
import io.modelcontextprotocol.spec.McpSchema;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class McpJobToolRegistry {
  private final JobService jobs;
  private final JobExportService exports;
  private final ObjectMapper json;

  public McpJobToolRegistry(JobService jobs, JobExportService exports, ObjectMapper json) {
    this.jobs = jobs; this.exports = exports; this.json = json;
  }

  public List<String> toolNames() { return List.of("search_jobs", "get_job", "export_jobs"); }

  public List<SyncToolSpecification> tools() {
    return List.of(tool("search_jobs", "Search published jobs.", this::searchJson), tool("get_job", "Get one published job.", this::getJson),
        tool("export_jobs", "Export published jobs as Markdown or base64 XLSX.", this::exportJson));
  }

  public String searchJson(Map<String, Object> arguments) { return write(jobs.list(filter(arguments))); }
  public String getJson(Map<String, Object> arguments) { return write(jobs.detail(id(arguments))); }
  public String exportJson(Map<String, Object> arguments) {
    String format = string(arguments, "format");
    if (format == null || format.equals("md")) return exports.markdown(filter(arguments));
    if (format.equals("xlsx")) return Base64.getEncoder().encodeToString(exports.xlsx(filter(arguments)));
    throw new IllegalArgumentException("format must be md or xlsx");
  }

  private SyncToolSpecification tool(String name, String description, java.util.function.Function<Map<String, Object>, String> action) {
    McpSchema.JsonSchema schema = new McpSchema.JsonSchema("object", Map.ofEntries(
        Map.entry("id", Map.of("type", "integer")), Map.entry("format", Map.of("type", "string")),
        Map.entry("keyword", Map.of("type", "string")), Map.entry("companyCategory", Map.of("type", "string")),
        Map.entry("company", Map.of("type", "string")), Map.entry("jobType", Map.of("type", "string")),
        Map.entry("city", Map.of("type", "string")), Map.entry("techStack", Map.of("type", "string")),
        Map.entry("certificate", Map.of("type", "string")), Map.entry("deadlineFrom", Map.of("type", "string")),
        Map.entry("deadlineTo", Map.of("type", "string")), Map.entry("sourceTrust", Map.of("type", "string")),
        Map.entry("changedSince", Map.of("type", "string"))), List.of(), false, null, null);
    McpSchema.Tool definition = McpSchema.Tool.builder().name(name).description(description).inputSchema(schema).build();
    return new SyncToolSpecification(definition, (exchange, request) -> result(action, request.arguments() == null ? Map.of() : request.arguments()));
  }

  private McpSchema.CallToolResult result(java.util.function.Function<Map<String, Object>, String> action, Map<String, Object> arguments) {
    try { return McpSchema.CallToolResult.builder().content(List.of(new McpSchema.TextContent(action.apply(arguments)))).build(); }
    catch (RuntimeException error) { return McpSchema.CallToolResult.builder().isError(true).content(List.of(new McpSchema.TextContent(error.getMessage()))).build(); }
  }

  private JobRepository.JobFilter filter(Map<String, Object> values) {
    return new JobRepository.JobFilter(string(values, "keyword"), string(values, "companyCategory"), string(values, "company"), string(values, "jobType"),
        string(values, "city"), string(values, "techStack"), string(values, "certificate"), string(values, "deadlineFrom"), string(values, "deadlineTo"),
        string(values, "sourceTrust"), string(values, "changedSince"), 1, 100);
  }

  private long id(Map<String, Object> values) { try { return Long.parseLong(String.valueOf(values.get("id"))); } catch (Exception error) { throw new IllegalArgumentException("id must be a number"); } }
  private String string(Map<String, Object> values, String key) { Object value = values.get(key); return value == null ? null : String.valueOf(value); }
  private String write(Object value) { try { return json.writeValueAsString(value); } catch (JsonProcessingException error) { throw new IllegalStateException("Could not encode MCP result", error); } }
}
