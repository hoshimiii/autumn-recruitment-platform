package dev.recruitops;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessException;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class ApiContractTest {
  @Autowired MockMvc mvc;
  @Autowired JdbcTemplate jdbc;

  @BeforeEach
  void reset() {
    jdbc.update("DELETE FROM audit_logs");
    jdbc.update("DELETE FROM job_snapshots");
    jdbc.update("DELETE FROM review_items");
    jdbc.update("DELETE FROM jobs");
    jdbc.update("DELETE FROM collection_runs");
    jdbc.update("DELETE FROM manual_items");
    jdbc.update("DELETE FROM sources");
  }

  @Test
  void manualItemsUseFrontendContractAndValidateRequiredFields() throws Exception {
    String body = """
        {"platform":"Campus portal","companyName":"Acme","jobTitle":"Java Developer",
        "searchKeywords":["Java","Spring"],"sourceUrl":"https://careers.example.com/jobs/1",
        "techStackKeywords":["Java","Spring Boot"],"certificateNotes":"CET-6",
        "requiredAction":"Submit profile","requiresLogin":true,"requiresCaptcha":false,
        "applyMethod":"PORTAL","reminderAt":"2026-08-01T09:00:00Z","priority":2,
        "status":"PENDING","notes":"Prepare transcript"}
        """;
    mvc.perform(post("/api/v1/manual-items").contentType(MediaType.APPLICATION_JSON).content(body))
        .andExpect(status().isCreated()).andExpect(jsonPath("platform").value("Campus portal"))
        .andExpect(jsonPath("searchKeywords[1]").value("Spring"))
        .andExpect(jsonPath("techStackKeywords[1]").value("Spring Boot"))
        .andExpect(jsonPath("requiresLogin").value(true));
    mvc.perform(post("/api/v1/manual-items").contentType(MediaType.APPLICATION_JSON)
        .content("{\"platform\":\"Official site\",\"companyName\":\"Later\",\"requiredAction\":\"Review\"}"))
        .andExpect(status().isCreated());
    mvc.perform(get("/api/v1/manual-items"))
        .andExpect(status().isOk()).andExpect(jsonPath("items", hasSize(2)))
        .andExpect(jsonPath("items[0].companyName").value("Later"));
    mvc.perform(post("/api/v1/manual-items").contentType(MediaType.APPLICATION_JSON)
        .content("{\"platform\":\"\",\"companyName\":\"\",\"requiredAction\":\"\"}"))
        .andExpect(status().isBadRequest()).andExpect(jsonPath("error.code").value("validation_error"));
  }

  @Test
  void migratesAndHealthIsReady() throws Exception {
    mvc.perform(get("/api/v1/health"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("status").value("UP"))
        .andExpect(jsonPath("database").value("UP"))
        .andExpect(jsonPath("scheduler").value("READY"));
  }

  @Test
  void sourceStoresOnlyEnvironmentReference() throws Exception {
    String body = """
        {"name":"Official careers","type":"COMPANY","baseUrl":"https://careers.example.com",
        "listUrl":"https://careers.example.com/jobs","cron":"0 0 8 * * *","timezone":"Asia/Shanghai",
        "minIntervalSeconds":7,"maxPages":2,"maxItems":25,"timeoutSeconds":12,
        "adapterType":"HTML","mode":"HTML","complianceStatus":"ALLOWED","trustLevel":"OFFICIAL",
        "companyCategory":"internet","selectors":"{\\"item\\":\\"article\\"}",
        "fieldMapping":"{\\"companyName\\":\\"$const:Acme\\"}",
        "termsUrl":"https://careers.example.com/terms","termsConfirmedAt":"2026-07-25T00:00:00Z",
        "robotsUrl":"https://careers.example.com/robots.txt","aiEnabled":true,
        "aiProvider":"openai-compatible","aiModel":"local-model","aiSecretRef":"RECRUITOPS_AI_KEY"}
        """;
    mvc.perform(post("/api/v1/sources").contentType(MediaType.APPLICATION_JSON).content(body))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("companyCategory").value("internet"))
        .andExpect(jsonPath("enabled").value(false))
        .andExpect(jsonPath("listUrl").value("https://careers.example.com/jobs"))
        .andExpect(jsonPath("cron").value("0 0 8 * * *"))
        .andExpect(jsonPath("timezone").value("Asia/Shanghai"))
        .andExpect(jsonPath("minIntervalSeconds").value(7))
        .andExpect(jsonPath("maxPages").value(2))
        .andExpect(jsonPath("maxItems").value(25))
        .andExpect(jsonPath("timeoutSeconds").value(12))
        .andExpect(jsonPath("selectors").value("{\"item\":\"article\"}"))
        .andExpect(jsonPath("fieldMapping").value("{\"companyName\":\"$const:Acme\"}"))
        .andExpect(jsonPath("termsConfirmedAt").value("2026-07-25T00:00:00Z"))
        .andExpect(jsonPath("aiEnabled").value(true))
        .andExpect(jsonPath("aiProvider").value("openai-compatible"))
        .andExpect(jsonPath("aiModel").value("local-model"))
        .andExpect(jsonPath("secretEnvRef").doesNotExist());
    mvc.perform(get("/api/v1/sources"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("items", hasSize(1)))
        .andExpect(jsonPath("items[0].secretEnvRef").doesNotExist());
    assertEquals("RECRUITOPS_AI_KEY",
        jdbc.queryForObject("SELECT secret_env_ref FROM sources", String.class));
    assertEquals("internet", jdbc.queryForObject("SELECT scope FROM sources", String.class));
    assertEquals(0, jdbc.queryForObject("SELECT enabled FROM sources", Integer.class));
  }

  @Test
  void sourceRejectsUnsafeBaseUrl() throws Exception {
    String body = "{\"name\":\"Unsafe\",\"type\":\"COMPANY\",\"baseUrl\":\"ftp://example.com\","
        + "\"adapterType\":\"HTML\",\"mode\":\"HTML\",\"complianceStatus\":\"ALLOWED\","
        + "\"trustLevel\":\"OFFICIAL\",\"companyCategory\":\"internet\"}";
    mvc.perform(post("/api/v1/sources").contentType(MediaType.APPLICATION_JSON).content(body))
        .andExpect(status().isBadRequest()).andExpect(jsonPath("error.code").value("validation_error"));
  }

  @Test
  void sqliteRejectsInvalidForeignKeys() {
    assertThrows(DataAccessException.class, () -> jdbc.update(
        "INSERT INTO collection_runs (source_id, trigger_type, status) VALUES (99999, 'MANUAL', 'SUCCESS')"));
  }

  @Test
  void corsAllowsConfiguredFrontendPreflight() throws Exception {
    mvc.perform(options("/api/v1/jobs").header("Origin", "http://127.0.0.1:5173")
        .header("Access-Control-Request-Method", "GET"))
        .andExpect(status().isOk())
        .andExpect(header().string("Access-Control-Allow-Origin", "http://127.0.0.1:5173"));
    mvc.perform(options("/api/v1/jobs").header("Origin", "https://untrusted.example")
        .header("Access-Control-Request-Method", "GET"))
        .andExpect(status().isForbidden());
  }

  @Test
  void sourceCanBeUpdatedAndDeleted() throws Exception {
    long id = insertSource();
    assertEquals(0, jdbc.queryForObject("SELECT enabled FROM sources WHERE id = ?", Integer.class, id));
    String body = "{\"name\":\"Updated\",\"type\":\"COMPANY\",\"baseUrl\":\"https://careers.example.com\",\"adapterType\":\"HTML\",\"mode\":\"HTML\",\"complianceStatus\":\"ALLOWED\",\"trustLevel\":\"OFFICIAL\",\"companyCategory\":\"internet\",\"enabled\":false}";
    mvc.perform(put("/api/v1/sources/{id}", id).contentType(MediaType.APPLICATION_JSON).content(body))
        .andExpect(status().isOk()).andExpect(jsonPath("configVersion").value(2));
    mvc.perform(delete("/api/v1/sources/{id}", id)).andExpect(status().isNoContent());
    mvc.perform(get("/api/v1/sources")).andExpect(jsonPath("items", hasSize(0)));
  }

  @Test
  void listsPublishedJobsWithFiltersAndReturnsDetail() throws Exception {
    long acmeId = insertJob("Acme", "Java Developer", "Shanghai", "[\"Java\",\"Spring Boot\"]", "[\"CET-6\"]");
    insertJob("Other", "Designer", "Beijing", "[]", "[]");
    mvc.perform(get("/api/v1/jobs").param("keyword", "Java").param("city", "Shanghai")
        .param("techStack", "Java").param("certificate", "CET-6"))
        .andExpect(status().isOk()).andExpect(jsonPath("total").value(1))
        .andExpect(jsonPath("items[0].companyName").value("Acme"))
        .andExpect(jsonPath("items[0].companyCategory").value("INTERNET"))
        .andExpect(jsonPath("items[0].jobFamily").value("ENGINEERING"))
        .andExpect(jsonPath("items[0].priority").value(2))
        .andExpect(jsonPath("items[0].sourceName").value("Official careers"))
        .andExpect(jsonPath("items[0].sourceType").value("COMPANY"))
        .andExpect(jsonPath("items[0].sourceTrust").value("OFFICIAL"))
        .andExpect(jsonPath("items[0].applyMethod").value("PORTAL"))
        .andExpect(jsonPath("items[0].requiresLogin").value(true))
        .andExpect(jsonPath("items[0].requiresCaptcha").value(false))
        .andExpect(jsonPath("items[0].reviewStatus").value("PUBLISHED"))
        .andExpect(jsonPath("items[0].confidence").value(0.95));
    mvc.perform(get("/api/v1/jobs/{id}", acmeId)).andExpect(status().isOk())
        .andExpect(jsonPath("companyName").value("Acme"))
        .andExpect(jsonPath("capturedAt").value("2026-07-25T00:00:00Z"))
        .andExpect(jsonPath("updatedAt").exists())
        .andExpect(jsonPath("requirements").value("Java"));
    mvc.perform(get("/api/v1/jobs/99999")).andExpect(status().isNotFound())
        .andExpect(jsonPath("error.code").value("not_found"));
  }

  @Test
  void approvingReviewIsIdempotentAndAudited() throws Exception {
    long sourceId = insertSource();
    long reviewId = insertReview(sourceId);
    mvc.perform(post("/api/v1/review-items/{id}/approve", reviewId))
        .andExpect(status().isOk()).andExpect(jsonPath("companyName").value("Acme"));
    mvc.perform(post("/api/v1/review-items/{id}/approve", reviewId)).andExpect(status().isOk());
    assertEquals(1, count("jobs"));
    assertEquals(1, count("job_snapshots"));
    assertEquals(1, count("audit_logs"));
  }

  @Test
  void reviewListReturnsFrontendFields() throws Exception {
    long reviewId = insertReview(insertSource());
    jdbc.update("UPDATE review_items SET review_notes = 'official link pending confirmation' WHERE id = ?", reviewId);
    mvc.perform(get("/api/v1/review-items"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("items", hasSize(1)))
        .andExpect(jsonPath("items[0].sourceId").exists())
        .andExpect(jsonPath("items[0].sourceName").value("Official careers"))
        .andExpect(jsonPath("items[0].companyName").value("Acme"))
        .andExpect(jsonPath("items[0].title").value("Java Developer"))
        .andExpect(jsonPath("items[0].reason").value("official link pending confirmation"))
        .andExpect(jsonPath("items[0].confidence").value(0.95))
        .andExpect(jsonPath("items[0].status").value("PENDING_REVIEW"))
        .andExpect(jsonPath("items[0].createdAt").exists())
        .andExpect(jsonPath("items[0].normalizedPayload.companyName").value("Acme"));
  }

  @Test
  void rejectingReviewDoesNotPublish() throws Exception {
    long reviewId = insertReview(insertSource());
    mvc.perform(post("/api/v1/review-items/{id}/reject", reviewId)
        .contentType(MediaType.APPLICATION_JSON).content("{\"reason\":\"missing official link\"}"))
        .andExpect(status().isOk()).andExpect(jsonPath("status").value("REJECTED"));
    mvc.perform(get("/api/v1/jobs")).andExpect(status().isOk()).andExpect(jsonPath("total").value(0));
  }

  @Test
  void collectionEndpointsPersistSynchronousRunHistory() throws Exception {
    long sourceId = insertSource();
    mvc.perform(post("/api/v1/sources/{id}/runs", sourceId))
        .andExpect(status().isOk()).andExpect(jsonPath("status").value("BLOCKED"));
    mvc.perform(post("/api/v1/sources/{id}/test", sourceId))
        .andExpect(status().isOk()).andExpect(jsonPath("status").value("BLOCKED"));
    mvc.perform(post("/api/v1/runs/daily"))
        .andExpect(status().isOk()).andExpect(jsonPath("status").value("SUCCESS"));
    mvc.perform(get("/api/v1/runs"))
        .andExpect(status().isOk()).andExpect(jsonPath("items", hasSize(3)));
  }

  private long insertJob(String company, String title, String city, String tech, String certificates) {
    long sourceId = insertSource();
    jdbc.update("""
        INSERT INTO jobs (source_id, company_type, company_name, title, job_family, job_type, city, deadline,
        status, priority, tech_stack, certificate_requirements, source_url, apply_url, apply_method,
        requires_login, requires_captcha, source_trust_level, requirements) VALUES (?, ?, ?, ?, 'ENGINEERING',
        'FULL_TIME', ?, '2099-12-31', 'PUBLISHED', 2, ?, ?, 'https://careers.example.com/jobs',
        'https://careers.example.com/apply', 'PORTAL', 1, 0, 'OFFICIAL', 'Java')""",
        sourceId, "INTERNET", company, title, city, tech, certificates);
    long jobId = jdbc.queryForObject("SELECT id FROM jobs WHERE company_name = ?", Long.class, company);
    jdbc.update("""
        INSERT INTO job_snapshots (job_id, source_id, source_url, content_hash, captured_at)
        VALUES (?, ?, 'https://careers.example.com/jobs', 'test-hash', '2026-07-25T00:00:00Z')""", jobId, sourceId);
    jdbc.update("""
        INSERT INTO review_items (source_id, candidate_payload, normalized_payload, dedupe_key, confidence, status,
        published_job_id) VALUES (?, '{\"companyName\":\"Acme\"}', '{\"companyName\":\"Acme\"}', ?, 0.95,
        'PUBLISHED', ?)""", sourceId, company + "-review", jobId);
    return jobId;
  }

  private long insertSource() {
    jdbc.update("""
        INSERT INTO sources (name, type, base_url, adapter_type, mode, compliance_status, trust_level, scope)
        VALUES ('Official careers', 'COMPANY', 'https://careers.example.com', 'HTML', 'HTML', 'ALLOWED', 'OFFICIAL', 'internet')""");
    return jdbc.queryForObject("SELECT last_insert_rowid()", Long.class);
  }

  private long insertReview(long sourceId) {
    String payload = "{\"companyName\":\"Acme\",\"companyType\":\"INTERNET\",\"title\":\"Java Developer\",\"jobType\":\"FULL_TIME\",\"city\":\"Shanghai\",\"sourceUrl\":\"https://careers.example.com/jobs/java\",\"applyUrl\":\"https://careers.example.com/apply\",\"techStack\":[\"Java\"],\"certificateRequirements\":[\"CET-6\"],\"jd\":\"Build services\",\"requirements\":\"Java\",\"deadline\":\"2099-12-31\",\"sourceTrustLevel\":\"OFFICIAL\"}";
    jdbc.update("""
        INSERT INTO review_items (source_id, candidate_payload, normalized_payload, dedupe_key, confidence, status)
        VALUES (?, ?, ?, 'acme-java', 0.95, 'PENDING_REVIEW')""", sourceId, payload, payload);
    return jdbc.queryForObject("SELECT id FROM review_items LIMIT 1", Long.class);
  }

  private int count(String table) { return jdbc.queryForObject("SELECT COUNT(*) FROM " + table, Integer.class); }
}
