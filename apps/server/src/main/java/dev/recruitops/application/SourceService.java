package dev.recruitops.application;

import dev.recruitops.domain.Source;
import dev.recruitops.infrastructure.JsonValues;
import dev.recruitops.infrastructure.SourceRepository;
import java.net.URI;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Service;

@Service
public class SourceService {
  private final SourceRepository sources;
  private final JsonValues json;
  public SourceService(SourceRepository sources, JsonValues json) { this.sources = sources; this.json = json; }
  public List<Source> list() { return sources.list(); }
  public Source create(SourceInput input) { return sources.create(input.toSource(0, json), validateSecret(input.secretEnvRef())); }
  public Source update(long id, SourceInput input) { return sources.update(id, input.toSource(id, json), validateSecret(input.secretEnvRef())); }
  public void delete(long id) { sources.delete(id); }
  private String validateSecret(String secret) {
    if (secret == null || secret.isBlank()) return null;
    if (!secret.matches("[A-Z][A-Z0-9_]*")) throw new IllegalArgumentException("secretEnvRef must be an environment variable name");
    return secret;
  }
  private static URI validateUrl(String name, String value) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException(name + " must be an absolute http or https URL without userinfo");
    }
    try {
      URI uri = URI.create(value);
      if (!uri.isAbsolute() || uri.getUserInfo() != null || uri.getHost() == null
          || !("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme()))) {
        throw new IllegalArgumentException(name + " must be an absolute http or https URL without userinfo");
      }
      return uri;
    } catch (IllegalArgumentException error) {
      throw new IllegalArgumentException(name + " must be an absolute http or https URL without userinfo");
    }
  }
  private static boolean sameOrigin(URI left, URI right) {
    return left.getScheme().equalsIgnoreCase(right.getScheme())
        && left.getHost().equalsIgnoreCase(right.getHost()) && port(left) == port(right);
  }
  private static int port(URI uri) {
    return uri.getPort() >= 0 ? uri.getPort() : ("https".equalsIgnoreCase(uri.getScheme()) ? 443 : 80);
  }
  public record SourceInput(
      String name, String type, String baseUrl, String listUrl, String adapterType, String mode,
      String complianceStatus, String trustLevel, String companyCategory, Boolean enabled, String cron,
      String timezone, Integer minIntervalSeconds, Integer maxPages, Integer maxItems, String selectors,
      String fieldMapping, String termsUrl, String termsConfirmedAt, String robotsUrl, Integer timeoutSeconds,
      Boolean aiEnabled, String aiProvider, String aiModel, String secretEnvRef) {
    Source toSource(long id, JsonValues json) {
      URI base = validateUrl("baseUrl", baseUrl);
      String configuredList = blank(listUrl) ? baseUrl : listUrl;
      URI list = validateUrl("listUrl", configuredList.replace("{page}", "1"));
      if (!sameOrigin(base, list)) throw new IllegalArgumentException("listUrl must use the source base origin");
      if (!blank(robotsUrl) && !sameOrigin(base, validateUrl("robotsUrl", robotsUrl))) {
        throw new IllegalArgumentException("robotsUrl must use the source base origin");
      }
      if (!blank(termsUrl)) validateUrl("termsUrl", termsUrl);
      String configuredCron = blank(cron) ? "0 0 8 * * *" : cron;
      String configuredTimezone = blank(timezone) ? "Asia/Shanghai" : timezone;
      try { CronExpression.parse(configuredCron); } catch (Exception error) {
        throw new IllegalArgumentException("cron is invalid");
      }
      try { ZoneId.of(configuredTimezone); } catch (Exception error) {
        throw new IllegalArgumentException("timezone is invalid");
      }
      if (!blank(termsConfirmedAt)) {
        try { Instant.parse(termsConfirmedAt); } catch (Exception error) {
          throw new IllegalArgumentException("termsConfirmedAt must be an ISO-8601 instant");
        }
      }
      if (Boolean.TRUE.equals(enabled)) {
        if (!"ALLOWED".equalsIgnoreCase(complianceStatus)
            || blank(termsUrl) || blank(termsConfirmedAt)) {
          throw new IllegalArgumentException(
              "enabled sources require ALLOWED compliance, termsUrl, and termsConfirmedAt");
        }
      }
      int interval = value(minIntervalSeconds, 5);
      int pages = value(maxPages, 1);
      int items = value(maxItems, 50);
      int timeout = value(timeoutSeconds, 15);
      if (interval < 0 || pages < 1 || items < 1 || timeout < 1) {
        throw new IllegalArgumentException("source limits are invalid");
      }
      return new Source(id, name, type, baseUrl, configuredList, adapterType, mode, complianceStatus,
          trustLevel, companyCategory, enabled != null && enabled, configuredCron, configuredTimezone,
          interval, pages, items, object(json, "selectors", selectors), object(json, "fieldMapping", fieldMapping),
          empty(termsUrl), empty(termsConfirmedAt), empty(robotsUrl), timeout, null, null, null, null,
          aiEnabled != null && aiEnabled, empty(aiProvider), empty(aiModel), 1);
    }
    private static int value(Integer value, int fallback) { return value == null ? fallback : value; }
    private static boolean blank(String value) { return value == null || value.isBlank(); }
    private static String empty(String value) { return blank(value) ? null : value; }
    private static String object(JsonValues json, String name, String value) {
      if (blank(value)) return "{}";
      try {
        if (json.tree(value).isObject()) return value;
      } catch (RuntimeException ignored) {
        // Converted to the stable validation error below.
      }
      throw new IllegalArgumentException(name + " must be a JSON object");
    }
  }
}
