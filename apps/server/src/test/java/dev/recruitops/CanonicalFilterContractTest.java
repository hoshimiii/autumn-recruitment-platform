package dev.recruitops;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.recruitops.cli.CliRunner;
import dev.recruitops.mcp.McpJobToolRegistry;
import dev.recruitops.infrastructure.JobRepository;
import io.modelcontextprotocol.spec.McpSchema;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.Map;
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
class CanonicalFilterContractTest {
  @Autowired MockMvc mvc;
  @Autowired JdbcTemplate jdbc;
  @Autowired ObjectMapper json;
  @Autowired CliRunner cli;
  @Autowired McpJobToolRegistry mcp;
  @Autowired JobRepository repository;

  @BeforeEach void fixtures() {
    jdbc.update("DELETE FROM jobs WHERE company_name IN ('FilterCompany', 'FilterOther')");
    insert("FilterCompany", "Platform", "Engineer", "2027", "MATCH", "PUBLISHED", "2099-12-31");
    insert("FilterCompany", "Sales", "Engineer", "2027", "OTHER_DEPT", "PUBLISHED", "2099-12-31");
    insert("FilterCompany", "Platform", "Designer", "2027", "OTHER_TITLE", "PUBLISHED", "2099-12-31");
    insert("FilterCompany", "Platform", "Engineer", "2028", "OTHER_YEAR", "PUBLISHED", "2099-12-31");
    insert("FilterCompany", null, "Engineer", null, "ABSENT", "PUBLISHED", "2099-12-31");
    insert("FilterOther", "Platform", "Engineer", "2027", "OTHER_COMPANY", "PUBLISHED", "2099-12-31");
    insert("FilterCompany", "Platform", "Engineer", "2027", "DRAFT", "DRAFT", "2099-12-31");
    insert("FilterCompany", "Platform", "Engineer", "2027", "EXPIRED", "PUBLISHED", "2000-01-01");
  }

  @Test void combinedExactFiltersSelectOnePublishedNonexpiredJob() throws Exception {
    var result = mvc.perform(get("/api/v1/jobs").param("company", "FilterCompany")
        .param("businessUnit", "Platform").param("title", "Engineer").param("recruitmentYear", "2027"))
        .andExpect(status().isOk()).andReturn();
    var page = json.readTree(result.getResponse().getContentAsString());
    assertEquals(1, page.path("total").asInt());
    assertEquals("MATCH", page.path("items").get(0).path("jd").asText());
  }

  @Test void blanksOmitOptionalPredicatesAndNullsDoNotMatchPresentFilters() throws Exception {
    var blank = mvc.perform(get("/api/v1/jobs").param("company", "FilterCompany")
        .param("businessUnit", "  ").param("title", "").param("recruitmentYear", "  "))
        .andExpect(status().isOk()).andReturn();
    var page = json.readTree(blank.getResponse().getContentAsString());
    assertEquals(5, page.path("total").asInt());
    var absent = java.util.stream.StreamSupport.stream(page.path("items").spliterator(), false)
        .filter(job -> job.path("jd").asText().equals("ABSENT")).findFirst().orElseThrow();
    assertTrue(absent.path("businessUnit").isNull());
    assertTrue(absent.path("recruitmentYear").isNull());
    assertEquals(3, total(Map.of("businessUnit", "Platform")));
    assertEquals(3, total(Map.of("recruitmentYear", 2027)));
    assertEquals(0, total(Map.of("businessUnit", "Plat")));
    assertEquals(0, total(Map.of("title", "Eng")));
    assertEquals(0, total(Map.of("recruitmentYear", "202")));
  }

  @Test void restAndCliAndMcpExportsKeepAllCombinedFilters() throws Exception {
    Map<String, Object> filters = Map.of("company", "FilterCompany", "businessUnit", "Platform", "title", "Engineer", "recruitmentYear", 2027);
    for (String format : java.util.List.of("md", "xlsx")) {
      var request = get("/api/v1/exports/jobs." + format);
      filters.forEach((key, value) -> request.param(key, value.toString()));
      byte[] body = mvc.perform(request).andExpect(status().isOk()).andReturn().getResponse().getContentAsByteArray();
      if (format.equals("md")) assertMarkdown(new String(body, java.nio.charset.StandardCharsets.UTF_8));
      else assertWorkbook(body);

      Path output = Files.createTempFile("canonical-filters-", "." + format);
      try {
        var args = new java.util.ArrayList<String>(java.util.List.of("export", "--format", format, "--output", output.toString()));
        filters.forEach((key, value) -> { args.add("--" + key); args.add(value.toString()); });
        assertEquals(0, cli.run(args.toArray(String[]::new), new ByteArrayOutputStream(), new ByteArrayOutputStream()));
        if (format.equals("md")) assertMarkdown(Files.readString(output));
        else assertWorkbook(Files.readAllBytes(output));
      } finally { Files.deleteIfExists(output); }

      var arguments = new java.util.HashMap<>(filters); arguments.put("format", format);
      var result = mcp.tools().get(2).callHandler().apply(null, new McpSchema.CallToolRequest("export_jobs", arguments));
      assertNotEquals(Boolean.TRUE, result.isError());
      String text = ((McpSchema.TextContent) result.content().get(0)).text();
      if (format.equals("md")) assertMarkdown(text); else assertWorkbook(Base64.getDecoder().decode(text));
    }
    var args = new java.util.ArrayList<String>(java.util.List.of("search"));
    filters.forEach((key, value) -> { args.add("--" + key); args.add(value.toString()); });
    var stdout = new ByteArrayOutputStream();
    assertEquals(0, cli.run(args.toArray(String[]::new), stdout, new ByteArrayOutputStream()));
    assertEquals(1, json.readTree(stdout.toByteArray()).path("total").asInt());
    var searched = mcp.tools().get(0).callHandler().apply(null, new McpSchema.CallToolRequest("search_jobs", filters));
    assertNotEquals(Boolean.TRUE, searched.isError());
    assertEquals(1, json.readTree(((McpSchema.TextContent) searched.content().get(0)).text()).path("total").asInt());
    assertTrue(mcp.tools().get(0).tool().inputSchema().properties().keySet().containsAll(filters.keySet()));
  }

  @Test void elasticHitIdsStillUseCanonicalSqlFiltersAndTotals() {
    var ids = jdbc.queryForList("SELECT id FROM jobs WHERE company_name IN ('FilterCompany', 'FilterOther')", Long.class);
    var filter = new JobRepository.JobFilter("unmatched literal", null, "FilterCompany", null, null,
        null, null, null, null, null, null, 1, 20, "Platform", "Engineer", "2027");
    var page = repository.list(filter, ids);
    assertEquals(1, page.total());
    assertEquals("MATCH", page.items().get(0).jd());
    assertEquals(0, repository.list(filter, java.util.List.of()).total());
  }

  private int total(Map<String, Object> optional) throws Exception {
    var filters = new java.util.HashMap<>(optional); filters.put("company", "FilterCompany");
    return json.readTree(mcp.searchJson(filters)).path("total").asInt();
  }
  private void assertMarkdown(String text) {
    assertTrue(text.contains("MATCH"));
    assertEquals(3, text.lines().count());
    assertFalse(text.contains("OTHER_")); assertFalse(text.contains("ABSENT"));
    assertFalse(text.contains("DRAFT")); assertFalse(text.contains("EXPIRED"));
  }
  private void assertWorkbook(byte[] bytes) throws Exception {
    try (var workbook = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
      var sheet = workbook.getSheetAt(0);
      assertEquals(1, sheet.getLastRowNum());
      assertEquals("MATCH", sheet.getRow(1).getCell(9).getStringCellValue());
      assertEquals("Platform", sheet.getRow(1).getCell(18).getStringCellValue());
      assertEquals("2027", sheet.getRow(1).getCell(19).getStringCellValue());
    }
  }

  private void insert(String company, String department, String title, String year, String marker, String state, String deadline) {
    jdbc.update("""
        INSERT INTO jobs (company_type,company_name,business_unit,title,job_type,campus_batch,jd,status,deadline)
        VALUES ('INTERNET',?,?,?,'FULL_TIME',?,?,?,?)
        """, company, department, title, year, marker, state, deadline);
  }
}
