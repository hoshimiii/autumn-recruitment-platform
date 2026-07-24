package dev.recruitops;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.recruitops.application.ReviewService;
import dev.recruitops.domain.CollectionRun;
import dev.recruitops.domain.Job;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class CollectorAdapterDedupeTest extends CollectorTestSupport {
  private static final String CONFIRMED = "2026-07-25T00:00:00Z";
  @Autowired ReviewService reviews;

  @Test
  void jsonExtractionDeduplicatesAndChangedApprovalUpdatesJob() {
    AtomicReference<String> body = new AtomicReference<>(jsonBody("Java", "Build services"));
    AtomicReference<String> userAgent = new AtomicReference<>();
    route("/robots.txt", 404, "text/plain", () -> "", null);
    route("/json", exchange -> {
      userAgent.set(exchange.getRequestHeaders().getFirst("User-Agent"));
      respond(exchange, 200, "application/json", body.get());
    });
    String selectors = "{\"itemsPath\":\"data.items\"}";
    long sourceId = insertSource("/json", "JSON", true, "ALLOWED", CONFIRMED, 1,
        selectors, mapping().formatted(origin));

    CollectionRun first = workflow.runSource(sourceId);
    String firstHash = jdbc.queryForObject(
        "SELECT content_hash FROM review_items ORDER BY id DESC LIMIT 1", String.class);
    long firstReview = jdbc.queryForObject(
        "SELECT id FROM review_items ORDER BY id DESC LIMIT 1", Long.class);
    Job created = reviews.approve(firstReview, null);

    CollectionRun duplicate = workflow.runSource(sourceId);
    body.set(jsonBody("Build services and tooling", "Build services"));
    CollectionRun changed = workflow.runSource(sourceId);
    String secondHash = jdbc.queryForObject(
        "SELECT content_hash FROM review_items ORDER BY id DESC LIMIT 1", String.class);
    long changedReview = jdbc.queryForObject(
        "SELECT id FROM review_items ORDER BY id DESC LIMIT 1", Long.class);
    Job updated = reviews.approve(changedReview, null);

    assertEquals("SUCCESS", first.status());
    assertEquals(1, first.discoveredCount());
    assertEquals(1, first.reviewItemCount());
    assertEquals(0, duplicate.reviewItemCount());
    assertEquals(1, changed.reviewItemCount());
    assertNotEquals(firstHash, secondHash);
    assertEquals(created.id(), updated.id());
    assertEquals(1, count("jobs"));
    assertEquals(2, count("job_snapshots"));
    assertEquals(2, count("audit_logs"));
    assertEquals("Build services and tooling",
        jdbc.queryForObject("SELECT requirements FROM jobs", String.class));
    assertEquals("Java Developer", jdbc.queryForObject(
        "SELECT json_extract(normalized_payload, '$.title') FROM review_items LIMIT 1", String.class));
    assertEquals("AutumnRecruitmentBot/1.0", userAgent.get());
  }

  @Test
  void htmlExtractionUsesConfiguredSelectorsWithoutDetailRequests() {
    route("/robots.txt", 200, "text/plain", () -> "User-agent: *\nAllow: /", null);
    route("/html", 200, "text/html", () -> """
        <html><body>
          <article><a class="title" href="/jobs/7"> Backend Engineer </a>
            <span class="city">Shenzhen</span><time>2099-08-01</time>
            <p class="detail">Build Java services</p></article>
          <article><a class="title" href="/jobs/8">Ignored by maxItems</a></article>
        </body></html>
        """, null);
    String selectors = """
        {"item":"article","title":".title","href":".title","city":".city",
        "date":"time","detail":".detail"}
        """;
    String mapping = """
        {"companyName":"$const:Acme","jobFamily":"$const:ENGINEERING",
        "jobType":"$const:FULL_TIME","sourceTrust":"$const:OFFICIAL"}
        """;
    long sourceId = insertSource("/html", "HTML", true, "ALLOWED", CONFIRMED, 1,
        selectors, mapping);

    CollectionRun run = workflow.runSource(sourceId);

    assertEquals("SUCCESS", run.status(), run.errorSummary());
    assertEquals(1, run.discoveredCount());
    assertEquals("Backend Engineer", jdbc.queryForObject(
        "SELECT json_extract(normalized_payload, '$.title') FROM review_items", String.class));
    assertEquals("Shenzhen", jdbc.queryForObject(
        "SELECT json_extract(normalized_payload, '$.city') FROM review_items", String.class));
    assertEquals(origin + "/jobs/7", jdbc.queryForObject(
        "SELECT json_extract(normalized_payload, '$.sourceUrl') FROM review_items", String.class));
  }

  @Test
  void sourceTestParsesButDoesNotPersistCandidates() {
    route("/robots.txt", 200, "text/plain", () -> "User-agent: *\nAllow: /", null);
    route("/json", 200, "application/json", () -> jsonBody("Java", "Build services"), null);
    long sourceId = insertSource("/json", "JSON", false, "ALLOWED", CONFIRMED, 1,
        "{\"itemsPath\":\"data.items\"}", mapping().formatted(origin));

    CollectionRun run = workflow.testSource(sourceId);

    assertEquals("SUCCESS", run.status(), run.errorSummary());
    assertEquals(1, run.discoveredCount());
    assertEquals(0, run.reviewItemCount());
    assertEquals(0, count("review_items"));
  }

  @Test
  void conditionalRequestPersistsValidatorsAndHandlesNotModified() {
    route("/robots.txt", 404, "text/plain", () -> "", null);
    route("/conditional", exchange -> {
      if ("v1".equals(exchange.getRequestHeaders().getFirst("If-None-Match"))) {
        respond(exchange, 304, null, "");
      } else {
        exchange.getResponseHeaders().set("ETag", "v1");
        exchange.getResponseHeaders().set("Last-Modified", "Fri, 25 Jul 2026 00:00:00 GMT");
        respond(exchange, 200, "application/json", jsonBody("Java", "Build services"));
      }
    });
    long sourceId = insertSource("/conditional", "JSON", true, "ALLOWED", CONFIRMED, 5,
        "{\"itemsPath\":\"data.items\"}", mapping().formatted(origin));

    workflow.runSource(sourceId);
    CollectionRun second = workflow.runSource(sourceId);

    assertEquals("NOT_MODIFIED", second.status());
    assertEquals("v1", jdbc.queryForObject("SELECT etag FROM sources WHERE id=?", String.class, sourceId));
    assertTrue(jdbc.queryForObject(
        "SELECT last_modified FROM sources WHERE id=?", String.class, sourceId).contains("Jul"));
  }

  @Test
  void missingCriticalFieldMarksRunParseFailed() {
    route("/robots.txt", 404, "text/plain", () -> "", null);
    route("/invalid", 200, "application/json", () -> "{\"items\":[{\"id\":\"1\"}]}", null);
    long sourceId = insertSource("/invalid", "JSON", true, "ALLOWED", CONFIRMED, 5,
        "{\"itemsPath\":\"items\"}", "{\"sourceUrlTemplate\":\"%s/jobs/{id}\"}".formatted(origin));

    CollectionRun run = workflow.runSource(sourceId);

    assertEquals("PARSE_FAILED", run.status());
    assertEquals(0, count("review_items"));
  }

  private String jsonBody(String requirements, String jd) {
    return """
        {"data":{"items":[
          {"id":"1","title":" Java Developer ","city":" Shanghai ","family":"ENGINEERING",
          "deadline":"2099-12-31","techStack":["Java","Spring Boot"],"certificates":["CET-6"],
          "jd":"%s","requirements":"%s","applyUrl":"%s/apply/1"},
          {"id":"2","title":"Ignored by maxItems","city":"Beijing"}
        ]}}
        """.formatted(jd, requirements, origin);
  }
}
