package dev.recruitops.application;

import dev.recruitops.domain.Source;
import dev.recruitops.infrastructure.SourceRepository;
import java.net.URI;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class SourceService {
  private final SourceRepository sources;
  public SourceService(SourceRepository sources) { this.sources = sources; }
  public List<Source> list() { return sources.list(); }
  public Source create(SourceInput input) { return sources.create(input.toSource(0), validateSecret(input.secretEnvRef())); }
  public Source update(long id, SourceInput input) { return sources.update(id, input.toSource(id), validateSecret(input.secretEnvRef())); }
  public void delete(long id) { sources.delete(id); }
  private String validateSecret(String secret) {
    if (secret == null || secret.isBlank()) return null;
    if (!secret.matches("[A-Z][A-Z0-9_]*")) throw new IllegalArgumentException("secretEnvRef must be an environment variable name");
    return secret;
  }
  private static String validateBaseUrl(String baseUrl) {
    if (baseUrl == null || baseUrl.isBlank()) {
      throw new IllegalArgumentException("baseUrl must be an absolute http or https URL without userinfo");
    }
    try {
      URI uri = URI.create(baseUrl);
      if (!uri.isAbsolute() || uri.getUserInfo() != null || uri.getHost() == null
          || !("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme()))) {
        throw new IllegalArgumentException("baseUrl must be an absolute http or https URL without userinfo");
      }
      return baseUrl;
    } catch (IllegalArgumentException error) {
      throw new IllegalArgumentException("baseUrl must be an absolute http or https URL without userinfo");
    }
  }
  public record SourceInput(String name, String type, String baseUrl, String adapterType, String mode,
      String complianceStatus, String trustLevel, String companyCategory, Boolean enabled, String secretEnvRef) {
    Source toSource(long id) { return new Source(id, name, type, validateBaseUrl(baseUrl), adapterType, mode, complianceStatus,
        trustLevel, companyCategory, enabled != null && enabled, 1); }
  }
}
