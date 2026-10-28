package dev.recruitops;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.recruitops.domain.CollectionRun;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class DailyWorkflowTest extends CollectorTestSupport {
  private static final String CONFIRMED = "2026-07-25T00:00:00Z";

  @Test
  void oneSourceFailureDoesNotPreventRemainingSources() {
    route("/robots.txt", 200, "text/plain", () -> "User-agent: *\nAllow: /", null);
    route("/bad", 500, "text/plain", () -> "failed", null);
    route("/good", 200, "application/json", () -> """
        {"items":[{"id":"1","title":"Java Developer","city":"Shanghai"}]}
        """, null);
    String mapping = """
        {"companyName":"$const:Acme","title":"title","city":"city",
        "jobType":"$const:FULL_TIME","sourceTrust":"$const:OFFICIAL",
        "sourceUrlTemplate":"%s/jobs/{id}"}
        """.formatted(origin);
    insertSource("/bad", "JSON", true, "ALLOWED", CONFIRMED, 5,
        "{\"itemsPath\":\"items\"}", mapping);
    insertSource("/good", "JSON", true, "ALLOWED", CONFIRMED, 5,
        "{\"itemsPath\":\"items\"}", mapping);

    CollectionRun daily = workflow.runDaily();

    assertEquals("PARTIAL_FAILURE", daily.status());
    assertEquals(1, daily.reviewItemCount());
    assertEquals(1, count("review_items"));
    assertEquals(3, count("collection_runs"));
    assertEquals(1, jdbc.queryForObject(
        "SELECT COUNT(*) FROM sources WHERE last_status='SUCCESS'", Integer.class));
    assertEquals(1, jdbc.queryForObject(
        "SELECT COUNT(*) FROM sources WHERE last_status='FAILED'", Integer.class));
  }

  @Test
  void dailyRunsCannotOverlapInOneProcess() throws Exception {
    CountDownLatch requestStarted = new CountDownLatch(1);
    CountDownLatch releaseRequest = new CountDownLatch(1);
    route("/robots.txt", 200, "text/plain", () -> "User-agent: *\nAllow: /", null);
    route("/slow", exchange -> {
      requestStarted.countDown();
      try { releaseRequest.await(5, TimeUnit.SECONDS); }
      catch (InterruptedException error) { Thread.currentThread().interrupt(); }
      respond(exchange, 200, "application/json",
          "{\"items\":[{\"id\":\"1\",\"title\":\"Java Developer\",\"city\":\"Shanghai\"}]}");
    });
    String mapping = """
        {"companyName":"$const:Acme","title":"title","city":"city",
        "jobType":"$const:FULL_TIME","sourceTrust":"$const:OFFICIAL",
        "sourceUrlTemplate":"%s/jobs/{id}"}
        """.formatted(origin);
    insertSource("/slow", "JSON", true, "ALLOWED", CONFIRMED, 5,
        "{\"itemsPath\":\"items\"}", mapping);
    ExecutorService executor = Executors.newSingleThreadExecutor();
    try {
      Future<CollectionRun> first = executor.submit(workflow::runDaily);
      assertTrue(requestStarted.await(5, TimeUnit.SECONDS));

      assertThrows(IllegalStateException.class, workflow::runDaily);
      releaseRequest.countDown();
      assertEquals("SUCCESS", first.get(5, TimeUnit.SECONDS).status());
    } finally {
      releaseRequest.countDown();
      executor.shutdownNow();
    }
  }

  @Test void stalledBodyReleasesDailyRunAndNextRunSucceeds() throws Exception {
    route("/robots.txt", 200, "text/plain", () -> "User-agent: *\nAllow: /", null);
    var attempts = new java.util.concurrent.atomic.AtomicInteger();
    CountDownLatch socketClosed = new CountDownLatch(1);
    route("/stream", exchange -> {
      if (attempts.incrementAndGet() > 1) {
        respond(exchange, 200, "application/json", "{\"items\":[]}");
        return;
      }
      exchange.sendResponseHeaders(200, 0);
      try {
        while (true) {
          exchange.getResponseBody().write(' '); exchange.getResponseBody().flush(); Thread.sleep(100);
        }
      } catch (java.io.IOException expected) { socketClosed.countDown(); }
      catch (InterruptedException error) { Thread.currentThread().interrupt(); }
      finally { exchange.close(); }
    });
    long id = insertSource("/stream", "JSON", true, "ALLOWED", CONFIRMED, 5,
        "{\"itemsPath\":\"items\"}", "{}");
    jdbc.update("UPDATE sources SET timeout_seconds=1 WHERE id=?", id);
    org.junit.jupiter.api.Assertions.assertTimeoutPreemptively(java.time.Duration.ofSeconds(5), () -> {
      assertEquals("PARTIAL_FAILURE", workflow.runDaily().status());
      assertTrue(socketClosed.await(2, TimeUnit.SECONDS));
      assertEquals("SUCCESS", workflow.runDaily().status());
    });
  }
}
