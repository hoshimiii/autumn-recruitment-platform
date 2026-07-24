package dev.recruitops.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dev.recruitops.domain.DiscoveredCandidate;
import dev.recruitops.domain.Source;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class CandidateNormalizer {
  private final ObjectMapper json;
  public CandidateNormalizer(ObjectMapper json) { this.json = json; }

  public DiscoveredCandidate normalize(Source source, Map<String, Object> raw) {
    String companyName = required(raw, "companyName");
    String title = required(raw, "title");
    String sourceUrl = canonicalUrl(required(raw, "sourceUrl"));
    String sourceTrust = first(text(raw, "sourceTrust"), text(raw, "sourceTrustLevel"),
        source.trustLevel());
    if (blank(sourceTrust)) throw CollectionException.parseFailed("source_trust_missing", null);
    ObjectNode value = json.createObjectNode();
    put(value, "companyCategory", first(text(raw, "companyCategory"),
        text(raw, "companyType"), source.companyCategory()));
    put(value, "companyName", clean(companyName));
    put(value, "title", clean(title));
    put(value, "jobFamily", clean(text(raw, "jobFamily")));
    put(value, "jobType", first(clean(text(raw, "jobType")), "UNKNOWN"));
    put(value, "city", clean(text(raw, "city")));
    put(value, "deadline", clean(text(raw, "deadline")));
    value.set("techStack", array(values(raw.get("techStack"))));
    value.set("certificateRequirements", array(values(raw.get("certificateRequirements"))));
    put(value, "sourceUrl", sourceUrl);
    put(value, "applyUrl", optionalUrl(text(raw, "applyUrl"), sourceUrl));
    put(value, "sourceTrust", clean(sourceTrust));
    put(value, "jd", clean(text(raw, "jd")));
    put(value, "requirements", clean(text(raw, "requirements")));
    value.put("priority", integer(raw.get("priority")));
    put(value, "applyMethod", clean(text(raw, "applyMethod")));
    value.put("requiresLogin", bool(raw.get("requiresLogin")));
    value.put("requiresCaptcha", bool(raw.get("requiresCaptcha")));
    String dedupe = identity(value.path("companyName").asText(), value.path("title").asText(),
        value.path("city").asText(), sourceUrl);
    return new DiscoveredCandidate(raw, value, sha256(value.toString()), dedupe, 1.0);
  }

  private ArrayNode array(List<String> values) {
    ArrayNode array = json.createArrayNode();
    values.stream().map(CandidateNormalizer::clean).filter(value -> !blank(value))
        .distinct().forEach(array::add);
    return array;
  }

  private static List<String> values(Object value) {
    if (value == null) return List.of();
    if (value instanceof List<?> list) {
      return list.stream().filter(java.util.Objects::nonNull).map(Object::toString).toList();
    }
    if (value instanceof JsonNode node && node.isArray()) {
      List<String> result = new ArrayList<>();
      node.forEach(item -> result.add(item.asText()));
      return result;
    }
    String text = value.toString();
    if (text.isBlank()) return List.of();
    return java.util.Arrays.stream(text.split("[,，;；]")).map(String::trim).toList();
  }

  private static String required(Map<String, Object> raw, String field) {
    String value = text(raw, field);
    if (blank(value)) throw CollectionException.parseFailed(field + "_missing", null);
    return value;
  }

  private static String text(Map<String, Object> raw, String field) {
    Object value = raw.get(field);
    return value == null ? null : value.toString();
  }

  private static int integer(Object value) {
    if (value instanceof Number number) return number.intValue();
    try { return value == null ? 0 : Integer.parseInt(value.toString()); }
    catch (NumberFormatException ignored) { return 0; }
  }

  private static boolean bool(Object value) {
    return value instanceof Boolean flag ? flag : value != null && Boolean.parseBoolean(value.toString());
  }

  private static void put(ObjectNode node, String field, String value) {
    if (value == null) node.putNull(field); else node.put(field, value);
  }

  private static String identity(String company, String title, String city, String sourceUrl) {
    return String.join("|", key(company), key(title), key(city), key(sourceUrl));
  }

  private static String key(String value) {
    String cleaned = clean(value);
    return cleaned == null ? "" : cleaned.toLowerCase(Locale.ROOT).replaceAll("\\s+", "");
  }

  private static String clean(String value) {
    if (value == null) return null;
    return Normalizer.normalize(value, Normalizer.Form.NFKC).trim().replaceAll("\\s+", " ");
  }

  private static String canonicalUrl(String value) {
    try {
      URI uri = URI.create(value).normalize();
      if (uri.getHost() == null || !(uri.getScheme().equalsIgnoreCase("http")
          || uri.getScheme().equalsIgnoreCase("https"))) {
        throw new IllegalArgumentException();
      }
      int port = uri.getPort();
      boolean defaultPort = port == -1 || port == 80 && uri.getScheme().equalsIgnoreCase("http")
          || port == 443 && uri.getScheme().equalsIgnoreCase("https");
      String path = uri.getPath();
      if (path != null && path.length() > 1 && path.endsWith("/")) path = path.substring(0, path.length() - 1);
      return new URI(uri.getScheme().toLowerCase(Locale.ROOT), null,
          uri.getHost().toLowerCase(Locale.ROOT), defaultPort ? -1 : port, path,
          uri.getQuery(), null).toString();
    } catch (Exception error) {
      throw CollectionException.parseFailed("source_url_invalid", error);
    }
  }

  private static String optionalUrl(String value, String base) {
    return blank(value) ? null : canonicalUrl(URI.create(base).resolve(value).toString());
  }

  private static String sha256(String value) {
    try {
      byte[] digest = MessageDigest.getInstance("SHA-256")
          .digest(value.getBytes(StandardCharsets.UTF_8));
      return java.util.HexFormat.of().formatHex(digest);
    } catch (Exception error) {
      throw new IllegalStateException("SHA-256 unavailable", error);
    }
  }

  private static String first(String... values) {
    for (String value : values) if (!blank(value)) return value;
    return null;
  }

  private static boolean blank(String value) { return value == null || value.isBlank(); }
}
