package dev.recruitops.infrastructure;

import dev.recruitops.domain.Source;
import java.util.List;
import java.util.Map;

public interface ContentAdapter {
  List<Map<String, Object>> parse(Source source, FetchResult content);
}
