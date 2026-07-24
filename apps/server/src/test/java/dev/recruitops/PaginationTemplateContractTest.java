package dev.recruitops;

import static org.junit.jupiter.api.Assertions.assertEquals;

import dev.recruitops.application.CollectionWorkflow;
import dev.recruitops.application.SourceService;
import dev.recruitops.domain.CollectionRun;
import dev.recruitops.domain.Source;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class PaginationTemplateContractTest extends CollectorTestSupport {
  @Autowired SourceService sources;
  @Autowired CollectionWorkflow workflow;

  @Test
  void sourceTemplateKeepsItsValueAndRunsEveryConfiguredPage() {
    route("/robots.txt", 200, "text/plain", () -> "User-agent: *\nAllow: /", null);
    route("/jobs", exchange -> respond(exchange, 200, "application/json",
        "page=2".equals(exchange.getRequestURI().getQuery())
            ? "{\"items\":[{\"id\":\"two\",\"title\":\"Second\",\"city\":\"Jinan\"}]}"
            : "{\"items\":[{\"id\":\"one\",\"title\":\"First\",\"city\":\"Qingdao\"}]}"));
    Source source = sources.create(new SourceService.SourceInput("Paged fixture", "COMPANY", origin,
        origin + "/jobs?page={page}", "JSON", "JSON", "ALLOWED", "OFFICIAL", "STATE_OWNED",
        true, null, null, 0, 2, 10, "{\"itemsPath\":\"items\"}", mapping().formatted(origin),
        null, "2026-07-25T00:00:00Z", origin + "/robots.txt", 3, false, null, null, null));

    CollectionRun run = workflow.runSource(source.id());

    assertEquals(origin + "/jobs?page={page}", source.listUrl());
    assertEquals("SUCCESS", run.status(), run.errorSummary());
    assertEquals(2, run.discoveredCount());
    assertEquals(2, run.reviewItemCount());
  }
}
