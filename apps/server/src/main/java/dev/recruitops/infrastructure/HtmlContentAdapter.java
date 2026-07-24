package dev.recruitops.infrastructure;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.recruitops.application.CollectionException;
import dev.recruitops.domain.Source;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Component;

@Component
public class HtmlContentAdapter implements ContentAdapter {
  private final ObjectMapper json;
  public HtmlContentAdapter(ObjectMapper json) { this.json = json; }

  @Override
  public List<Map<String, Object>> parse(Source source, FetchResult content) {
    try {
      JsonNode selectors = json.readTree(source.selectors());
      String itemSelector = required(selectors, "item");
      var document = Jsoup.parse(new String(content.body(), StandardCharsets.UTF_8),
          content.uri().toString());
      List<Map<String, Object>> parsed = new ArrayList<>();
      for (Element item : document.select(itemSelector)) {
        Map<String, Object> values = constants(source.fieldMapping());
        values.put("title", text(item, selectors.path("title").asText("text")));
        values.put("sourceUrl", href(item, required(selectors, "href")));
        optionalText(values, "city", item, selectors.path("city").asText(null));
        optionalText(values, "deadline", item, selectors.path("date").asText(null));
        optionalText(values, "jd", item, selectors.path("detail").asText(null));
        parsed.add(values);
      }
      return parsed;
    } catch (CollectionException error) {
      throw error;
    } catch (Exception error) {
      throw CollectionException.parseFailed("html_parse_failed", error);
    }
  }

  private Map<String, Object> constants(String mappingJson) throws Exception {
    JsonNode mapping = json.readTree(mappingJson);
    Map<String, Object> values = new LinkedHashMap<>();
    Iterator<Map.Entry<String, JsonNode>> fields = mapping.fields();
    while (fields.hasNext()) {
      Map.Entry<String, JsonNode> field = fields.next();
      String expression = field.getValue().asText();
      if (expression.startsWith("$const:")) {
        values.put(field.getKey(), expression.substring("$const:".length()));
      }
    }
    return values;
  }

  private static String required(JsonNode selectors, String field) {
    String value = selectors.path(field).asText("");
    if (value.isBlank()) throw CollectionException.parseFailed("html_" + field + "_selector_missing", null);
    return value;
  }

  private static String text(Element item, String selector) {
    if (selector == null || selector.isBlank() || selector.equals("text")) return item.text();
    Element selected = item.selectFirst(selector);
    return selected == null ? null : selected.text();
  }

  private static String href(Element item, String selector) {
    Element selected = selector.equals("href") ? item : item.selectFirst(selector);
    return selected == null ? null : selected.absUrl("href");
  }

  private static void optionalText(Map<String, Object> values, String field, Element item,
                                   String selector) {
    if (selector != null && !selector.isBlank()) values.put(field, text(item, selector));
  }
}
