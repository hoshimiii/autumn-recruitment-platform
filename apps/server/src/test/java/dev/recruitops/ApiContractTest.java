package dev.recruitops;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
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
    jdbc.update("DELETE FROM sources");
  }

  @Test
  void migratesAndHealthIsReady() throws Exception {
    mvc.perform(get("/api/v1/health"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("status").value("UP"))
        .andExpect(jsonPath("database").value("UP"));
  }

  @Test
  void sourceStoresOnlyEnvironmentReference() throws Exception {
    String body = """
        {"name":"Official careers","type":"COMPANY","baseUrl":"https://careers.example.com",
        "adapterType":"HTML","mode":"HTML","complianceStatus":"ALLOWED","trustLevel":"OFFICIAL",
        "scope":"internet","secretEnvRef":"RECRUITOPS_AI_KEY"}
        """;
    mvc.perform(post("/api/v1/sources").contentType(MediaType.APPLICATION_JSON).content(body))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("secretEnvRef").doesNotExist());
    mvc.perform(get("/api/v1/sources"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("items", hasSize(1)))
        .andExpect(jsonPath("items[0].secretEnvRef").doesNotExist());
    org.junit.jupiter.api.Assertions.assertEquals("RECRUITOPS_AI_KEY",
        jdbc.queryForObject("SELECT secret_env_ref FROM sources", String.class));
  }

  @Test
  void sourceCanBeUpdatedAndDeleted() throws Exception {
    long id = insertSource();
    String body = "{\"name\":\"Updated\",\"type\":\"COMPANY\",\"baseUrl\":\"https://careers.example.com\",\"adapterType\":\"HTML\",\"mode\":\"HTML\",\"complianceStatus\":\"ALLOWED\",\"trustLevel\":\"OFFICIAL\",\"scope\":\"internet\",\"enabled\":false}";
    mvc.perform(put("/api/v1/sources/{id}", id).contentType(MediaType.APPLICATION_JSON).content(body))
        .andExpect(status().isOk()).andExpect(jsonPath("configVersion").value(2));
    mvc.perform(delete("/api/v1/sources/{id}", id)).andExpect(status().isNoContent());
    mvc.perform(get("/api/v1/sources")).andExpect(jsonPath("items", hasSize(0)));
  }

  @Test
  void listsPublishedJobsWithFiltersAndReturnsDetail() throws Exception {
    insertJob("Acme", "Java Developer", "Shanghai", "[\"Java\",\"Spring Boot\"]", "[\"CET-6\"]");
    insertJob("Other", "Designer", "Beijing", "[]", "[]");
    mvc.perform(get("/api/v1/jobs").param("keyword", "Java").param("city", "Shanghai")
        .param("techStack", "Java").param("certificate", "CET-6"))
        .andExpect(status().isOk()).andExpect(jsonPath("total").value(1))
        .andExpect(jsonPath("items[0].companyName").value("Acme"));
    Long id = jdbc.queryForObject("SELECT id FROM jobs WHERE company_name='Acme'", Long.class);
    mvc.perform(get("/api/v1/jobs/{id}", id)).andExpect(status().isOk())
        .andExpect(jsonPath("companyName").value("Acme"));
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
    org.junit.jupiter.api.Assertions.assertEquals(1, count("jobs"));
    org.junit.jupiter.api.Assertions.assertEquals(1, count("job_snapshots"));
    org.junit.jupiter.api.Assertions.assertEquals(1, count("audit_logs"));
  }

  @Test
  void rejectingReviewDoesNotPublish() throws Exception {
    long reviewId = insertReview(insertSource());
    mvc.perform(post("/api/v1/review-items/{id}/reject", reviewId)
        .contentType(MediaType.APPLICATION_JSON).content("{\"reason\":\"missing official link\"}"))
        .andExpect(status().isOk()).andExpect(jsonPath("status").value("REJECTED"));
    mvc.perform(get("/api/v1/jobs")).andExpect(status().isOk()).andExpect(jsonPath("total").value(0));
  }

  private void insertJob(String company, String title, String city, String tech, String certificates) {
    jdbc.update("""
        INSERT INTO jobs (company_type, company_name, title, job_type, city, deadline, status,
        tech_stack, certificate_requirements, source_url, source_trust_level) VALUES (?, ?, ?, 'FULL_TIME', ?,
        '2099-12-31', 'PUBLISHED', ?, ?, 'https://careers.example.com/jobs', 'OFFICIAL')""",
        "INTERNET", company, title, city, tech, certificates);
  }

  private long insertSource() {
    jdbc.update("""
        INSERT INTO sources (name, type, base_url, adapter_type, mode, compliance_status, trust_level, scope)
        VALUES ('Official careers', 'COMPANY', 'https://careers.example.com', 'HTML', 'HTML', 'ALLOWED', 'OFFICIAL', 'internet')""");
    return jdbc.queryForObject("SELECT id FROM sources LIMIT 1", Long.class);
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
