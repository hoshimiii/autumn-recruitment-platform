package dev.recruitops.infrastructure;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.recruitops.application.CollectionException;
import dev.recruitops.domain.Source;
import java.net.URI;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class JsonContentAdapter implements ContentAdapter {
  private static final Pattern TEMPLATE_FIELD = Pattern.compile("\\{([^}]+)}");
  private final ObjectMapper json;

  public JsonContentAdapter(ObjectMapper json) { this.json = json; }

  @Override
  public List<Map<String, Object>> parse(Source source, FetchResult content) {
    try {
      JsonNode root = json.readTree(content.body());
      JsonNode selectors = json.readTree(source.selectors());
      JsonNode mapping = json.readTree(source.fieldMapping());
      JsonNode items = path(root, selectors.path("itemsPath").asText(""));
      if (!items.isArray()) throw CollectionException.parseFailed("json_items_path_not_array", null);
      List<Map<String, Object>> parsed = new ArrayList<>();
      for (JsonNode item : items) parsed.add(mapItem(item, mapping, content.uri()));
      return parsed;
    } catch (CollectionException error) {
      throw error;
    } catch (Exception error) {
      throw CollectionException.parseFailed("json_parse_failed", error);
    }
  }

  private Map<String, Object> mapItem(JsonNode item, JsonNode mapping, URI pageUri) {
    Map<String, Object> values = new LinkedHashMap<>();
    if (mapping.isEmpty() && item.isObject()) {
      item.fields().forEachRemaining(entry -> values.put(entry.getKey(), javaValue(entry.getValue())));
    } else {
      Iterator<Map.Entry<String, JsonNode>> fields = mapping.fields();
      while (fields.hasNext()) {
        Map.Entry<String, JsonNode> field = fields.next();
        if (field.getKey().equals("sourceUrlTemplate")) continue;
        String expression = field.getValue().asText();
        if (expression.equals("$sourceUrl")) continue;
        values.put(field.getKey(), expression.startsWith("$const:")
            ? expression.substring("$const:".length()) : javaValue(path(item, expression)));
      }
      if (mapping.hasNonNull("sourceUrlTemplate")) {
        values.put("sourceUrl", template(mapping.get("sourceUrlTemplate").asText(), item));
      }
      fields = mapping.fields();
      while (fields.hasNext()) {
        Map.Entry<String, JsonNode> field = fields.next();
        if ("$sourceUrl".equals(field.getValue().asText())) {
          values.put(field.getKey(), values.get("sourceUrl"));
        }
      }
    }
    resolveUrl(values, "sourceUrl", pageUri);
    resolveUrl(values, "applyUrl", pageUri);
    return values;
  }

  private String template(String template, JsonNode item) {
    Matcher matcher = TEMPLATE_FIELD.matcher(template);
    StringBuffer result = new StringBuffer();
    while (matcher.find()) {
      String replacement = path(item, matcher.group(1)).asText("");
      matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
    }
    matcher.appendTail(result);
    return result.toString();
  }

  private static JsonNode path(JsonNode node, String path) {
    if (path == null || path.isBlank()) return node;
    JsonNode current = node;
    for (String segment : path.split("\\.")) {
      current = current.isArray() && segment.matches("\\d+")
          ? current.path(Integer.parseInt(segment)) : current.path(segment);
    }
    return current;
  }

  private Object javaValue(JsonNode value) {
    if (value == null || value.isMissingNode() || value.isNull()) return null;
    return json.convertValue(value, Object.class);
  }

  private static void resolveUrl(Map<String, Object> values, String field, URI pageUri) {
    Object value = values.get(field);
    if (value instanceof String text && !text.isBlank()) values.put(field, pageUri.resolve(text).toString());
  }
}
