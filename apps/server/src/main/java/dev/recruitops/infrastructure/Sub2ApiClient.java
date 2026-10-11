package dev.recruitops.infrastructure;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dev.recruitops.domain.AgentSettings;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpTimeoutException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.TreeSet;
import org.springframework.stereotype.Component;

@Component
public class Sub2ApiClient {
  private static final int MAX_BODY_BYTES = 1024 * 1024;
  private static final int MAX_OUTPUT_TOKENS = 2048;
  private final HttpClient client = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NEVER).build();
  private final ObjectMapper json;

  public Sub2ApiClient(ObjectMapper json) { this.json = json; }

  public CatalogResult catalog(AgentSettings settings, String secret) {
    try {
      HttpRequest request = HttpRequest.newBuilder(URI.create(settings.baseUrl() + "/models"))
          .timeout(Duration.ofSeconds(settings.timeoutSeconds())).header("Authorization", "Bearer " + secret)
          .header("Accept", "application/json").GET().build();
      HttpResponse<InputStream> response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());
      if (response.statusCode() < 200 || response.statusCode() >= 300) {
        response.body().close();
        return CatalogResult.unavailable();
      }
      JsonNode data = json.readTree(readBody(response.body())).path("data");
      if (!data.isArray()) return CatalogResult.unavailable();
      TreeSet<String> models = new TreeSet<>();
      for (JsonNode item : data) if (item.path("id").isTextual()) models.add(item.path("id").asText());
      return new CatalogResult(true, List.copyOf(models));
    } catch (IOException | InterruptedException | RuntimeException error) {
      if (error instanceof InterruptedException) Thread.currentThread().interrupt();
      return CatalogResult.unavailable();
    }
  }

  public ResponseResult responses(AgentSettings settings, String secret, String model, String effort,
                                  String instructions, String input) {
    try {
      ObjectNode payload = json.createObjectNode();
      payload.put("model", model);
      payload.put("instructions", instructions);
      payload.put("input", input);
      payload.put("store", false);
      payload.put("stream", false);
      payload.put("max_output_tokens", MAX_OUTPUT_TOKENS);
      payload.putObject("reasoning").put("effort", effort);
      payload.putArray("tools");
      HttpRequest request = HttpRequest.newBuilder(URI.create(settings.baseUrl() + "/responses"))
          .timeout(Duration.ofSeconds(settings.timeoutSeconds())).header("Authorization", "Bearer " + secret)
          .header("Accept", "application/json").header("Content-Type", "application/json")
          .POST(HttpRequest.BodyPublishers.ofString(json.writeValueAsString(payload), StandardCharsets.UTF_8)).build();
      HttpResponse<InputStream> response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());
      if (response.statusCode() < 200 || response.statusCode() >= 300) {
        response.body().close();
        return ResponseResult.failed("provider_http_error");
      }
      String output = outputText(readBody(response.body()));
      return output == null ? ResponseResult.failed("provider_response_invalid") : ResponseResult.success(output);
    } catch (BodyTooLarge error) {
      return ResponseResult.failed("provider_response_too_large");
    } catch (HttpTimeoutException error) {
      return ResponseResult.failed("provider_timeout");
    } catch (InterruptedException error) {
      Thread.currentThread().interrupt();
      return ResponseResult.failed("provider_interrupted");
    } catch (IOException | RuntimeException error) {
      return ResponseResult.failed("provider_unavailable");
    }
  }

  private String outputText(String body) throws IOException {
    JsonNode response = json.readTree(body);
    if (response.path("output_text").isTextual()) return response.path("output_text").asText();
    for (JsonNode output : response.path("output")) {
      for (JsonNode content : output.path("content")) {
        if (content.path("text").isTextual()) return content.path("text").asText();
      }
    }
    return null;
  }

  private static String readBody(InputStream input) throws IOException {
    try (input; ByteArrayOutputStream output = new ByteArrayOutputStream()) {
      byte[] buffer = new byte[8192];
      for (int read; (read = input.read(buffer)) != -1;) {
        if (output.size() + read > MAX_BODY_BYTES) throw new BodyTooLarge();
        output.write(buffer, 0, read);
      }
      return output.toString(StandardCharsets.UTF_8);
    }
  }

  private static final class BodyTooLarge extends IOException {}

  public record CatalogResult(boolean connected, List<String> models) {
    static CatalogResult unavailable() { return new CatalogResult(false, List.of()); }
  }
  public record ResponseResult(boolean successful, String output, String reason) {
    static ResponseResult success(String output) { return new ResponseResult(true, output, null); }
    static ResponseResult failed(String reason) { return new ResponseResult(false, null, reason); }
  }
}
