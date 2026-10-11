package dev.recruitops.application;

import dev.recruitops.domain.AgentSettings;
import dev.recruitops.infrastructure.AgentSettingsRepository;
import dev.recruitops.infrastructure.Sub2ApiClient;
import java.net.URI;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class AgentSettingsService {
  private static final Set<String> EFFORTS = Set.of("low", "medium", "high", "xhigh");
  private final AgentSettingsRepository settings;
  private final EnvironmentSecretResolver secrets;
  private final Sub2ApiClient sub2api;

  public AgentSettingsService(AgentSettingsRepository settings, EnvironmentSecretResolver secrets,
                              Sub2ApiClient sub2api) {
    this.settings = settings;
    this.secrets = secrets;
    this.sub2api = sub2api;
  }

  @org.springframework.security.access.prepost.PreAuthorize("@operationAuthorizer.can('ADMIN')")
  public SettingsResponse get() { return response(settings.get()); }

  @org.springframework.security.access.prepost.PreAuthorize("@operationAuthorizer.can('ADMIN')")
  public SettingsResponse update(SettingsInput input) {
    AgentSettings current = settings.get();
    AgentSettings updated = new AgentSettings(1, required(input.enabled(), "enabled"), localBaseUrl(input.baseUrl()),
        envRef(input.secretEnvRef()), requiredText(input.scanModel(), "scanModel"), effort(input.scanReasoningEffort()),
        requiredText(input.processModel(), "processModel"), effort(input.processReasoningEffort()),
        timeout(input.timeoutSeconds()), current.configVersion(), current.updatedAt());
    return response(settings.update(updated));
  }

  @org.springframework.security.access.prepost.PreAuthorize("@operationAuthorizer.can('ADMIN')")
  public CatalogCheck check() {
    AgentSettings current;
    try {
      current = validated(settings.get());
    } catch (IllegalArgumentException error) {
      return new CatalogCheck(false, false, List.of(), false, false, "Settings are invalid");
    }
    Optional<String> secret = secrets.resolve(current.secretEnvRef());
    if (secret.isEmpty()) return new CatalogCheck(false, false, List.of(), false, false, "Secret is not configured");
    Sub2ApiClient.CatalogResult result = sub2api.catalog(current, secret.get());
    if (!result.connected()) return new CatalogCheck(false, true, List.of(), false, false, "Model catalog unavailable");
    return new CatalogCheck(true, true, result.models(), result.models().contains(current.scanModel()),
        result.models().contains(current.processModel()), "Model catalog available");
  }

  ExecutionConfig executionConfig() {
    AgentSettings current = settings.get();
    if (!current.enabled()) return ExecutionConfig.disabled();
    try {
      AgentSettings safe = validated(current);
      return secrets.resolve(safe.secretEnvRef()).map(secret -> ExecutionConfig.ready(safe, secret))
          .orElseGet(() -> ExecutionConfig.unavailable(safe, "secret_missing"));
    } catch (IllegalArgumentException error) {
      return ExecutionConfig.unavailable(current, "settings_invalid");
    }
  }

  private SettingsResponse response(AgentSettings value) {
    return new SettingsResponse(value.enabled(), value.baseUrl(), value.secretEnvRef(),
        secrets.resolve(value.secretEnvRef()).isPresent(), value.scanModel(), value.scanReasoningEffort(),
        value.processModel(), value.processReasoningEffort(), value.timeoutSeconds(), value.configVersion(), value.updatedAt());
  }

  private static boolean required(Boolean value, String name) {
    if (value == null) throw new IllegalArgumentException(name + " is required");
    return value;
  }

  private static String requiredText(String value, String name) {
    if (value == null || value.isBlank()) throw new IllegalArgumentException(name + " must not be blank");
    return value.trim();
  }

  private static String envRef(String value) {
    String result = requiredText(value, "secretEnvRef");
    if (!result.matches("[A-Z][A-Z0-9_]*")) throw new IllegalArgumentException("secretEnvRef must be an environment variable name");
    return result;
  }

  private static String effort(String value) {
    String result = requiredText(value, "reasoningEffort").toLowerCase(Locale.ROOT);
    if (!EFFORTS.contains(result)) throw new IllegalArgumentException("reasoningEffort is invalid");
    return result;
  }

  private static int timeout(Integer value) {
    if (value == null || value < 1 || value > 120) throw new IllegalArgumentException("timeoutSeconds must be between 1 and 120");
    return value;
  }

  private static AgentSettings validated(AgentSettings value) {
    return new AgentSettings(value.id(), value.enabled(), localBaseUrl(value.baseUrl()), envRef(value.secretEnvRef()),
        requiredText(value.scanModel(), "scanModel"), effort(value.scanReasoningEffort()),
        requiredText(value.processModel(), "processModel"), effort(value.processReasoningEffort()),
        timeout(value.timeoutSeconds()), value.configVersion(), value.updatedAt());
  }

  private static String localBaseUrl(String value) {
    try {
      URI uri = URI.create(requiredText(value, "baseUrl"));
      String scheme = uri.getScheme();
      String host = uri.getHost();
      if (scheme == null || !(scheme.equalsIgnoreCase("http") || scheme.equalsIgnoreCase("https"))
          || uri.getUserInfo() != null || uri.getRawQuery() != null || uri.getRawFragment() != null || host == null
          || !"/v1".equals(uri.getPath()) && !"/v1/".equals(uri.getPath()) || !loopback(host)) {
        throw new IllegalArgumentException();
      }
      return new URI(scheme.toLowerCase(Locale.ROOT), null, host, uri.getPort(), "/v1", null, null).toString();
    } catch (RuntimeException | java.net.URISyntaxException error) {
      throw new IllegalArgumentException("baseUrl must be an HTTP(S) loopback /v1 URL without userinfo, query, or fragment");
    }
  }

  private static boolean loopback(String host) {
    String normalized = host.replace("[", "").replace("]", "").toLowerCase(Locale.ROOT);
    return normalized.equals("localhost") || normalized.equals("127.0.0.1") || normalized.equals("::1");
  }

  public record SettingsInput(Boolean enabled, String baseUrl, String secretEnvRef, String scanModel,
      String scanReasoningEffort, String processModel, String processReasoningEffort, Integer timeoutSeconds) {}
  public record SettingsResponse(boolean enabled, String baseUrl, String secretEnvRef, boolean secretConfigured,
      String scanModel, String scanReasoningEffort, String processModel, String processReasoningEffort,
      int timeoutSeconds, int configVersion, String updatedAt) {}
  public record CatalogCheck(boolean connected, boolean secretConfigured, List<String> models,
      boolean scanModelAvailable, boolean processModelAvailable, String message) {}

  static final class ExecutionConfig {
    private final AgentSettings settings;
    private final String secret;
    private final String unavailableReason;

    private ExecutionConfig(AgentSettings settings, String secret, String unavailableReason) {
      this.settings = settings;
      this.secret = secret;
      this.unavailableReason = unavailableReason;
    }

    static ExecutionConfig disabled() { return new ExecutionConfig(null, null, "agent_disabled"); }
    static ExecutionConfig ready(AgentSettings settings, String secret) {
      return new ExecutionConfig(settings, secret, null);
    }
    static ExecutionConfig unavailable(AgentSettings settings, String reason) {
      return new ExecutionConfig(settings, null, reason);
    }
    boolean enabled() { return settings != null; }
    boolean ready() { return secret != null; }
    AgentSettings settings() { return settings; }
    String secret() { return secret; }
    String unavailableReason() { return unavailableReason; }
  }
}
