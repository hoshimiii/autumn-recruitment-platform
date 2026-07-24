package dev.recruitops.infrastructure;

import dev.recruitops.application.CollectionException;
import org.springframework.stereotype.Component;

@Component
public class AdapterRegistry {
  private final JsonContentAdapter json;
  private final HtmlContentAdapter html;

  public AdapterRegistry(JsonContentAdapter json, HtmlContentAdapter html) {
    this.json = json;
    this.html = html;
  }

  public ContentAdapter adapter(String mode) {
    if ("JSON".equalsIgnoreCase(mode)) return json;
    if ("HTML".equalsIgnoreCase(mode)) return html;
    throw CollectionException.blocked("adapter_mode_not_supported");
  }
}
