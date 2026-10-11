package dev.recruitops.infrastructure;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.recruitops.domain.Job;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ElasticJobIndex {
  private static final int MAX_IDS = 1000;
  private static final int MAX_BYTES = 1024 * 1024;
  private final ObjectMapper json;
  private final String base;
  private final boolean enabled;
  private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofMillis(500))
      .followRedirects(HttpClient.Redirect.NEVER).build();
  private volatile boolean initialized;
  public ElasticJobIndex(ObjectMapper json,
      @Value("${recruitops.elasticsearch.url:http://127.0.0.1:19200}") String endpoint,
      @Value("${recruitops.elasticsearch.enabled:false}") boolean enabled, DatasetNamespace namespace) {
    URI uri = URI.create(endpoint);
    if (!"http".equals(uri.getScheme()) || !List.of("localhost", "127.0.0.1", "::1").contains(uri.getHost())
        || uri.getUserInfo() != null || uri.getQuery() != null || uri.getFragment() != null
        || uri.getPath() != null && !List.of("", "/").contains(uri.getPath()))
      throw new IllegalArgumentException("Elasticsearch must be a credential-free loopback HTTP endpoint");
    this.json = json; this.base = endpoint.replaceAll("/$", "") + "/recruitops-jobs-" + namespace.value(); this.enabled = enabled;
  }
  public boolean enabled() { return enabled; }
  public Optional<List<Long>> searchIds(String keyword) {
    if (!enabled || keyword == null || keyword.isBlank() || keyword.length() > 200) return Optional.empty();
    try {
      JsonNode reply = request("POST", "/_search", json.writeValueAsString(Map.of(
          "size", MAX_IDS + 1, "track_total_hits", true, "_source", false,
          "query", Map.of("multi_match", Map.of("query", keyword, "fields", List.of("companyName", "title^2", "jd", "businessUnit"),
              "fuzziness", "AUTO", "prefix_length", 1, "max_expansions", 25)))));
      JsonNode hits = reply.path("hits");
      if (!hits.path("hits").isArray() || !"eq".equals(hits.path("total").path("relation").asText())
          || hits.path("total").path("value").asLong(MAX_IDS + 1) > MAX_IDS) return Optional.empty();
      List<Long> ids = new ArrayList<>();
      for (JsonNode item : hits.path("hits")) {
        long id = Long.parseLong(item.path("_id").asText());
        if (id <= 0 || ids.size() >= MAX_IDS) return Optional.empty();
        ids.add(id);
      }
      return Optional.of(List.copyOf(ids));
    } catch (RuntimeException | java.io.IOException unavailable) { return Optional.empty(); }
  }
  public void upsert(Job job, long version) {
    if (!"PUBLISHED".equals(job.status())) throw new IllegalArgumentException("Only published jobs can be indexed");
    ensureIndex();
    try { request("PUT", "/_doc/" + job.id() + "?version=" + version + "&version_type=external_gte", json.writeValueAsString(job)); }
    catch (java.io.IOException error) { throw new IllegalStateException("Elasticsearch indexing unavailable", error); }
  }
  public void delete(long jobId, long version) {
    ensureIndex();
    request("DELETE", "/_doc/" + jobId + "?version=" + version + "&version_type=external_gte", null);
  }
  public void refresh() { request("POST", "/_refresh", null); }
  private synchronized void ensureIndex() {
    if (initialized) return;
    request("PUT", "", "{\"mappings\":{\"properties\":{\"companyName\":{\"type\":\"text\"},\"title\":{\"type\":\"text\"},\"jd\":{\"type\":\"text\"},\"businessUnit\":{\"type\":\"text\"}}}}");
    initialized = true;
  }
  private JsonNode request(String method, String path, String body) {
    try {
      HttpRequest request = HttpRequest.newBuilder(URI.create(base + path)).timeout(Duration.ofSeconds(2))
          .header("Content-Type", "application/json").method(method, body == null
              ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(body)).build();
      HttpResponse<InputStream> response = http.send(request, HttpResponse.BodyHandlers.ofInputStream());
      byte[] bytes;
      try (InputStream stream = response.body()) { bytes = stream.readNBytes(MAX_BYTES + 1); }
      if (bytes.length > MAX_BYTES) throw new IllegalStateException("Elasticsearch response exceeds limit");
      JsonNode value = bytes.length == 0 ? json.createObjectNode() : json.readTree(bytes);
      int status = response.statusCode();
      if (status >= 200 && status < 300 || method.equals("DELETE") && status == 404
          || status == 409 && path.startsWith("/_doc/") && path.contains("version_type=external_gte")
              && value.path("error").path("type").asText().equals("version_conflict_engine_exception")
          || method.equals("PUT") && path.isEmpty() && status == 400
              && value.path("error").path("type").asText().equals("resource_already_exists_exception")) return value;
      throw new IllegalStateException("Elasticsearch request failed (HTTP " + status + ")");
    } catch (InterruptedException error) {
      Thread.currentThread().interrupt(); throw new IllegalStateException("Elasticsearch request interrupted", error);
    } catch (java.io.IOException error) { throw new IllegalStateException("Elasticsearch request unavailable", error); }
  }
}
