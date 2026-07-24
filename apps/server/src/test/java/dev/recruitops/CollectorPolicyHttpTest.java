package dev.recruitops;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.recruitops.domain.CollectionRun;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class CollectorPolicyHttpTest extends CollectorTestSupport {
  private static final String CONFIRMED = "2026-07-25T00:00:00Z";

  @Test
  void policyBlocksBeforeAnyNetworkRequest() {
    AtomicInteger requests = new AtomicInteger();
    route("/robots.txt", 200, "text/plain", () -> "User-agent: *\nAllow: /", requests);
    route("/jobs", 200, "application/json", () -> "{\"items\":[]}", requests);
    long disabled = insertSource("/jobs", "JSON", false, "ALLOWED", CONFIRMED, 5,
        "{\"itemsPath\":\"items\"}", "{}");
    long unconfirmed = insertSource("/jobs", "JSON", false, "ALLOWED", null, 5,
        "{\"itemsPath\":\"items\"}", "{}");

    CollectionRun automatic = workflow.runSource(disabled);
    CollectionRun test = workflow.testSource(unconfirmed);

    assertEquals("BLOCKED", automatic.status());
    assertEquals("BLOCKED", test.status());
    assertTrue(test.errorSummary().contains("terms"));
    assertEquals(0, requests.get());
  }

  @Test
  void robotsDenyPreventsBusinessRequest() {
    AtomicInteger businessRequests = new AtomicInteger();
    route("/robots.txt", 200, "text/plain",
        () -> "User-agent: AutumnRecruitmentBot\nDisallow: /jobs", null);
    route("/jobs", 200, "application/json", () -> "{\"items\":[]}", businessRequests);
    long sourceId = insertSource("/jobs", "JSON", true, "allowed", CONFIRMED, 5,
        "{\"itemsPath\":\"items\"}", "{}");

    CollectionRun run = workflow.runSource(sourceId);

    assertEquals("BLOCKED", run.status(), run.errorSummary());
    assertEquals(0, businessRequests.get());
  }

  @Test
  void crossOriginRedirectIsBlockedBeforeFollowing() {
    AtomicInteger redirectedRequests = new AtomicInteger();
    route("/robots.txt", 200, "text/plain", () -> "User-agent: *\nAllow: /", null);
    route("/redirect", exchange -> {
      exchange.getResponseHeaders().set(
          "Location", "http://localhost:" + server.getAddress().getPort() + "/foreign");
      exchange.sendResponseHeaders(302, -1);
      exchange.close();
    });
    route("/foreign", 200, "application/json", () -> "{\"items\":[]}", redirectedRequests);
    long sourceId = insertSource("/redirect", "JSON", true, "ALLOWED", CONFIRMED, 5,
        "{\"itemsPath\":\"items\"}", "{}");

    CollectionRun run = workflow.runSource(sourceId);

    assertEquals("BLOCKED", run.status(), run.errorSummary());
    assertTrue(run.errorSummary().contains("origin"), run.errorSummary());
    assertEquals(0, redirectedRequests.get());
  }

  @Test
  void sameOriginRedirectIsFollowed() {
    AtomicInteger redirectedRequests = new AtomicInteger();
    route("/robots.txt", 200, "text/plain", () -> "User-agent: *\nAllow: /", null);
    route("/redirect", exchange -> {
      exchange.getResponseHeaders().set("Location", "/jobs");
      exchange.sendResponseHeaders(302, -1);
      exchange.close();
    });
    route("/jobs", 200, "application/json", () -> "{\"items\":[]}", redirectedRequests);
    long sourceId = insertSource("/redirect", "JSON", true, "ALLOWED", CONFIRMED, 5,
        "{\"itemsPath\":\"items\"}", "{}");

    CollectionRun run = workflow.runSource(sourceId);

    assertEquals("SUCCESS", run.status(), run.errorSummary());
    assertEquals(1, redirectedRequests.get());
  }

  @Test
  void rateLimitResponseStopsWithoutRetry() {
    AtomicInteger requests = new AtomicInteger();
    route("/robots.txt", 200, "text/plain", () -> "User-agent: *\nAllow: /", null);
    route("/limited", 429, "text/plain", () -> "rate limited", requests);
    long sourceId = insertSource("/limited", "JSON", true, "ALLOWED", CONFIRMED, 5,
        "{\"itemsPath\":\"items\"}", "{}");

    CollectionRun run = workflow.runSource(sourceId);

    assertEquals("BLOCKED", run.status(), run.errorSummary());
    assertEquals(1, requests.get());
  }

  @Test
  void robotsServerFailureFailsClosed() {
    AtomicInteger businessRequests = new AtomicInteger();
    route("/robots.txt", 503, "text/plain", () -> "unavailable", null);
    route("/jobs", 200, "application/json", () -> "{\"items\":[]}", businessRequests);
    long sourceId = insertSource("/jobs", "JSON", true, "ALLOWED", CONFIRMED, 5,
        "{\"itemsPath\":\"items\"}", "{}");

    CollectionRun run = workflow.runSource(sourceId);

    assertEquals("BLOCKED", run.status());
    assertEquals(0, businessRequests.get());
  }

  @Test
  void oversizedResponseIsRejected() {
    route("/robots.txt", 404, "text/plain", () -> "", null);
    route("/large", 200, "application/json",
        () -> "x".repeat(2 * 1024 * 1024 + 1), null);
    long sourceId = insertSource("/large", "JSON", true, "ALLOWED", CONFIRMED, 5,
        "{\"itemsPath\":\"items\"}", "{}");

    CollectionRun run = workflow.runSource(sourceId);

    assertEquals("FAILED", run.status());
    assertTrue(run.errorSummary().contains("response_body_too_large"));
  }

  @Test
  void loginContentIndicatorBlocksTheSource() {
    AtomicInteger requests = new AtomicInteger();
    route("/robots.txt", 404, "text/plain", () -> "", null);
    route("/protected", 200, "text/html",
        () -> "<html><title>Login</title><body>Login required</body></html>", requests);
    long sourceId = insertSource("/protected", "HTML", true, "ALLOWED", CONFIRMED, 5,
        "{\"item\":\"article\",\"href\":\"href\"}", "{}");

    CollectionRun run = workflow.runSource(sourceId);

    assertEquals("BLOCKED", run.status());
    assertEquals(1, requests.get());
  }
}
