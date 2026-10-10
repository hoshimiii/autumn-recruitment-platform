package dev.recruitops;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verifyNoInteractions;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.recruitops.application.EnvironmentSecretResolver;
import dev.recruitops.domain.CollectionRun;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

@SpringBootTest
class CollectorAgentWorkflowTest extends CollectorTestSupport {
  private static final String CONFIRMED = "2026-07-25T00:00:00Z";
  @Autowired ObjectMapper json;
  @MockBean EnvironmentSecretResolver secrets;

  @Test
  void enrichesCandidatesAndPersistsScannerEvidence() throws Exception {
    AtomicInteger calls = new AtomicInteger();
    List<JsonNode> requests = new ArrayList<>();
    route("/v1/responses", exchange -> {
      requests.add(json.readTree(exchange.getRequestBody().readAllBytes()));
      int call = calls.getAndIncrement();
      respond(exchange, 200, "application/json", output(call == 0
          ? "{\"items\":[{\"index\":0,\"relevant\":true,\"reason\":\"matches Java\",\"confidence\":0.9}]}"
          : "{\"items\":[{\"index\":0,\"fields\":{\"companyName\":\"Injected\",\"title\":\"Injected title\",\"sourceUrl\":\"https://attacker.invalid/source\",\"applyUrl\":\"https://attacker.invalid/apply\",\"city\":\"Beijing\",\"techStack\":[\"Java\",\"Spring\"],\"priority\":1}}]}"));
    });
    when(secrets.resolve("LOCAL_KEY")).thenReturn(Optional.of("provider-key"));
    configureAgent(true);
    long sourceId = source(true);

    CollectionRun run = workflow.runSource(sourceId);

    assertEquals("SUCCESS", run.status(), run.errorSummary());
    assertEquals(2, calls.get());
    assertEquals("scan-model", requests.get(0).path("model").asText());
    assertEquals("high", requests.get(0).path("reasoning").path("effort").asText());
    assertTrue(requests.get(0).path("instructions").asText().contains("untrusted data"));
    assertTrue(requests.get(0).path("instructions").asText().contains("confidence"));
    assertEquals("process-model", requests.get(1).path("model").asText());
    assertEquals("medium", requests.get(1).path("reasoning").path("effort").asText());
    assertTrue(requests.get(1).path("instructions").asText().contains("applyUrl"));
    assertTrue(requests.get(1).path("instructions").asText().contains("jobFamily"));
    assertFalse(requests.get(0).path("store").asBoolean(true));
    assertFalse(requests.get(0).path("stream").asBoolean(true));
    assertEquals("Beijing", jdbc.queryForObject(
        "SELECT json_extract(normalized_payload, '$.city') FROM review_items", String.class));
    assertEquals("Acme", jdbc.queryForObject(
        "SELECT json_extract(normalized_payload, '$.companyName') FROM review_items", String.class));
    assertEquals("Java Developer", jdbc.queryForObject(
        "SELECT json_extract(normalized_payload, '$.title') FROM review_items", String.class));
    assertEquals(origin + "/jobs/1", jdbc.queryForObject(
        "SELECT json_extract(normalized_payload, '$.sourceUrl') FROM review_items", String.class));
    assertEquals(origin + "/apply", jdbc.queryForObject(
        "SELECT json_extract(normalized_payload, '$.applyUrl') FROM review_items", String.class));
    String payload = jdbc.queryForObject("SELECT candidate_payload FROM review_items", String.class);
    assertTrue(payload.contains("\"status\":\"SUCCESS\""));
    assertTrue(payload.contains("\"scannerModel\":\"scan-model\""));
    assertTrue(payload.contains("\"processorModel\":\"process-model\""));
    assertEquals("PENDING_REVIEW", jdbc.queryForObject("SELECT status FROM review_items", String.class));
  }

  @Test
  void globalOrSourceDisableSkipsAgentRequests() throws Exception {
    AtomicInteger calls = new AtomicInteger();
    route("/v1/responses", 200, "application/json", () -> "{}", calls);
    when(secrets.resolve("LOCAL_KEY")).thenReturn(Optional.of("provider-key"));
    configureAgent(false);
    long sourceId = source(true);

    workflow.runSource(sourceId);
    configureAgent(true);
    jdbc.update("UPDATE sources SET ai_enabled=0 WHERE id=?", sourceId);
    workflow.runSource(sourceId);

    assertEquals(0, calls.get());
  }

  @Test
  void missingSecretFallsBackWithoutDataLoss() {
    AtomicInteger calls = new AtomicInteger();
    route("/v1/responses", 200, "application/json", () -> "{}", calls);
    configureAgent(true);
    CollectionRun run = workflow.runSource(source(true));

    assertEquals("SUCCESS", run.status(), run.errorSummary());
    assertEquals(0, calls.get());
    assertFallback("secret_missing");
  }

  @Test
  void tamperedGlobalSettingsFallBackBeforeResolvingTheSecret() {
    configureAgent(true);
    jdbc.update("UPDATE agent_settings SET base_url='https://example.com/v1',secret_env_ref='LOCAL_KEY'");

    CollectionRun run = workflow.runSource(source(true));

    assertEquals("SUCCESS", run.status(), run.errorSummary());
    assertFallback("settings_invalid");
    verifyNoInteractions(secrets);
  }

  @Test
  void providerFailureFallsBackWithoutLeakingKeyOrProviderBody() {
    AtomicInteger calls = new AtomicInteger();
    route("/v1/responses", 500, "application/json", () -> "{\"error\":\"provider-body-secret\"}", calls);
    when(secrets.resolve("LOCAL_KEY")).thenReturn(Optional.of("provider-key"));
    configureAgent(true);
    CollectionRun run = workflow.runSource(source(true));

    assertEquals("SUCCESS", run.status(), run.errorSummary());
    assertEquals(1, calls.get());
    assertFallback("provider_http_error");
    String payload = jdbc.queryForObject("SELECT candidate_payload FROM review_items", String.class);
    assertFalse(payload.contains("provider-key"));
    assertFalse(payload.contains("provider-body-secret"));
    assertNull(jdbc.queryForObject("SELECT error_summary FROM collection_runs", String.class));
  }

  @Test
  void malformedAgentOutputFallsBackWithoutDataLoss() throws Exception {
    AtomicInteger calls = new AtomicInteger();
    route("/v1/responses", 200, "application/json", () -> {
      calls.incrementAndGet();
      return output("not-json");
    }, null);
    when(secrets.resolve("LOCAL_KEY")).thenReturn(Optional.of("provider-key"));
    configureAgent(true);
    CollectionRun run = workflow.runSource(source(true));

    assertEquals("SUCCESS", run.status(), run.errorSummary());
    assertEquals(1, calls.get());
    assertFallback("agent_output_invalid");
  }

  private long source(boolean aiEnabled) {
    route("/robots.txt", 404, "text/plain", () -> "", null);
    route("/jobs", 200, "application/json", () -> """
        {"data":{"items":[{"id":"1","title":"Java Developer","city":"Shanghai",
        "family":"ENGINEERING","deadline":"2099-12-31","techStack":["Java"],
        "certificates":[],"jd":"Build services","requirements":"Java","applyUrl":"%s/apply"}]}}
        """.formatted(origin), null);
    long sourceId = insertSource("/jobs", "JSON", true, "ALLOWED", CONFIRMED, 1,
        "{\"itemsPath\":\"data.items\"}", mapping().formatted(origin));
    jdbc.update("UPDATE sources SET ai_enabled=? WHERE id=?", aiEnabled, sourceId);
    return sourceId;
  }

  private void configureAgent(boolean enabled) {
    jdbc.update("""
        UPDATE agent_settings SET enabled=?,base_url=?,secret_env_ref='LOCAL_KEY',scan_model='scan-model',
        scan_reasoning_effort='high',process_model='process-model',process_reasoning_effort='medium',
        timeout_seconds=3
        """, enabled, origin + "/v1");
  }

  private String output(String value) {
    try {
      return json.writeValueAsString(java.util.Map.of("output_text", value));
    } catch (Exception error) {
      throw new IllegalStateException(error);
    }
  }

  private void assertFallback(String reason) {
    assertEquals(1, count("review_items"));
    String payload = jdbc.queryForObject("SELECT candidate_payload FROM review_items", String.class);
    assertTrue(payload.contains("\"status\":\"FALLBACK\""));
    assertTrue(payload.contains("\"reason\":\"" + reason + "\""));
    assertEquals("Shanghai", jdbc.queryForObject(
        "SELECT json_extract(normalized_payload, '$.city') FROM review_items", String.class));
  }
}
