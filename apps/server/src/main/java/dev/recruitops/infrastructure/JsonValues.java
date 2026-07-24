package dev.recruitops.infrastructure;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class JsonValues {
  private final ObjectMapper mapper;
  public JsonValues(ObjectMapper mapper) { this.mapper = mapper; }
  public JsonNode tree(String value) { try { return mapper.readTree(value); } catch (Exception e) { throw new IllegalStateException("Invalid stored JSON", e); } }
  public List<String> strings(String value) { try { return mapper.readValue(value == null ? "[]" : value, new TypeReference<>() {}); } catch (Exception e) { throw new IllegalStateException("Invalid stored JSON", e); } }
  public Map<String, Object> map(Object value) { return mapper.convertValue(value, new TypeReference<>() {}); }
  public String write(Object value) { try { return mapper.writeValueAsString(value); } catch (Exception e) { throw new IllegalArgumentException("Invalid JSON payload", e); } }
}
