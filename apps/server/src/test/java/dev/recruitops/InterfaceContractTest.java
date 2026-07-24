package dev.recruitops;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import dev.recruitops.application.JobExportService;
import dev.recruitops.cli.CliRunner;
import dev.recruitops.infrastructure.JobRepository;
import dev.recruitops.mcp.McpJobToolRegistry;
import io.modelcontextprotocol.spec.McpSchema;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class InterfaceContractTest {
  @Autowired MockMvc mvc;
  @Autowired JdbcTemplate jdbc;
  @Autowired JobExportService exports;
  @Autowired CliRunner cli;
  @Autowired McpJobToolRegistry mcpTools;

  @BeforeEach
  void reset() {
    jdbc.update("DELETE FROM audit_logs");
    jdbc.update("DELETE FROM job_snapshots");
    jdbc.update("DELETE FROM review_items");
    jdbc.update("DELETE FROM jobs");
    jdbc.update("DELETE FROM collection_runs");
    jdbc.update("DELETE FROM sources");
  }

  @Test
  void canonicalMarkdownAndWorkbookKeepFieldOrderAndEscapeValues() throws Exception {
    insertJob("A|B", "Java Developer", "Shanghai", "React, Java", "CET-6");
    JobRepository.JobFilter filter = new JobRepository.JobFilter(null, null, null, null, null,
        null, null, null, null, null, null, 1, 20);
    String markdown = exports.markdown(filter);
    assertTrue(markdown.startsWith("| companyCategory | companyName | title | jobFamily | jobType | city | deadline | techStack | certificateRequirements | jd | requirements | applyUrl | sourceUrl | sourceTrust | capturedAt | confidence | reviewStatus | updatedAt |"));
    assertTrue(markdown.contains("A\\|B"));
    try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(exports.xlsx(filter)))) {
      assertEquals("companyCategory", workbook.getSheetAt(0).getRow(0).getCell(0).getStringCellValue());
      assertEquals("A|B", workbook.getSheetAt(0).getRow(1).getCell(1).getStringCellValue());
    }
  }

  @Test
  void restExportsUseJobFiltersAndDownloadHeaders() throws Exception {
    insertJob("Acme", "Java Developer", "Shanghai", "Java", "CET-6");
    insertJob("Other", "Designer", "Beijing", "Figma", "");
    mvc.perform(get("/api/v1/exports/jobs.md").param("city", "Shanghai"))
        .andExpect(status().isOk())
        .andExpect(header().string("Content-Type", "text/markdown;charset=UTF-8"))
        .andExpect(header().string("Content-Disposition", "attachment; filename=jobs.md"));
    mvc.perform(get("/api/v1/exports/jobs.xlsx").param("city", "Shanghai"))
        .andExpect(status().isOk())
        .andExpect(header().string("Content-Type", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
        .andExpect(header().string("Content-Disposition", "attachment; filename=jobs.xlsx"));
  }

  @Test
  void exportReadsEveryMatchingRepositoryPage() {
    for (int index = 0; index < 101; index++) insertJob("Company" + index, "Developer", "Shanghai", "Java", "");
    JobRepository.JobFilter filter = new JobRepository.JobFilter(null, null, null, null, "Shanghai",
        null, null, null, null, null, null, 1, 1);
    assertTrue(exports.markdown(filter).contains("Company100"));
  }

  @Test
  void cliSearchAndExportUseServicesAndRejectInvalidArguments() throws Exception {
    insertJob("Acme", "Java Developer", "Shanghai", "Java", "CET-6");
    Path output = Files.createTempFile("jobs-", ".md");
    ByteArrayOutputStream stdout = new ByteArrayOutputStream();
    ByteArrayOutputStream stderr = new ByteArrayOutputStream();
    int search = cli.run(new String[] {"search", "--city", "Shanghai"}, stdout, stderr);
    int export = cli.run(new String[] {"export", "--format", "md", "--output", output.toString()}, stdout, stderr);
    int invalid = cli.run(new String[] {"search", "--city"}, stdout, stderr);
    assertEquals(0, search);
    assertEquals(0, export);
    assertTrue(stdout.toString().contains("Acme"));
    assertTrue(Files.readString(output).contains("Acme"));
    assertEquals(2, invalid);
  }

  @Test
  void mcpRegistersOnlySafeToolsAndSearchReturnsJobServiceData() {
    insertJob("Acme", "Java Developer", "Shanghai", "Java", "CET-6");
    assertEquals(List.of("search_jobs", "get_job", "export_jobs"), mcpTools.toolNames());
    assertTrue(mcpTools.searchJson(java.util.Map.of("city", "Shanghai")).contains("Acme"));
    McpSchema.CallToolResult result = mcpTools.tools().get(0).callHandler().apply(null,
        new McpSchema.CallToolRequest("search_jobs", java.util.Map.of("city", "Shanghai")));
    assertTrue(((McpSchema.TextContent) result.content().get(0)).text().contains("Acme"));
  }

  private void insertJob(String company, String title, String city, String tech, String certificates) {
    jdbc.update("""
        INSERT INTO sources (name, type, base_url, adapter_type, mode, compliance_status, trust_level, scope)
        VALUES ('Official careers', 'COMPANY', 'https://careers.example.com', 'HTML', 'HTML', 'ALLOWED', 'OFFICIAL', 'internet')""");
    long sourceId = jdbc.queryForObject("SELECT last_insert_rowid()", Long.class);
    jdbc.update("""
        INSERT INTO jobs (source_id, company_type, company_name, title, job_family, job_type, city, deadline,
        status, priority, tech_stack, certificate_requirements, source_url, apply_url, source_trust_level, jd, requirements)
        VALUES (?, 'INTERNET', ?, ?, 'ENGINEERING', 'FULL_TIME', ?, '2099-12-31', 'PUBLISHED', 2, ?, ?,
        'https://careers.example.com/jobs', 'https://careers.example.com/apply', 'OFFICIAL', 'Build | services', 'Java')""",
        sourceId, company, title, city, jsonArray(tech), jsonArray(certificates));
    long jobId = jdbc.queryForObject("SELECT last_insert_rowid()", Long.class);
    jdbc.update("""
        INSERT INTO job_snapshots (job_id, source_id, source_url, content_hash, captured_at)
        VALUES (?, ?, 'https://careers.example.com/jobs', ?, '2026-07-25T00:00:00Z')""", jobId, sourceId, company + "-hash");
    jdbc.update("""
        INSERT INTO review_items (source_id, candidate_payload, normalized_payload, dedupe_key, confidence, status, published_job_id)
        VALUES (?, '{}', '{}', ?, 0.95, 'PUBLISHED', ?)""", sourceId, company + "-review", jobId);
  }

  private String jsonArray(String value) { return value.isBlank() ? "[]" : "[\"" + value + "\"]"; }
}
