package dev.recruitops.application;

import dev.recruitops.domain.Source;
import java.net.URI;
import org.springframework.stereotype.Component;

@Component
public class SourcePolicy {
  public void requireAllowed(Source source, boolean testMode) {
    if (!testMode && !source.enabled()) throw CollectionException.blocked("source_disabled");
    if ("MANUAL".equalsIgnoreCase(source.mode())) throw CollectionException.blocked("manual_only");
    if (!"ALLOWED".equalsIgnoreCase(source.complianceStatus())) {
      throw CollectionException.blocked("compliance_not_allowed");
    }
    if (blank(source.termsConfirmedAt())) throw CollectionException.blocked("terms_unconfirmed");
    if (blank(source.termsUrl())) throw CollectionException.blocked("terms_url_missing");
    URI base = valid("base_url", source.baseUrl());
    URI list = valid("list_url", source.listUrl().replace("{page}", "1"));
    if (!sameOrigin(base, list)) throw CollectionException.blocked("list_url_origin_not_allowed");
    if (accessBarrier(list)) throw CollectionException.blocked("login_or_captcha_url");
    if (!blank(source.robotsUrl())
        && !sameOrigin(base, valid("robots_url", source.robotsUrl()))) {
      throw CollectionException.blocked("robots_url_origin_not_allowed");
    }
    valid("terms_url", source.termsUrl());
  }

  public static URI valid(String field, String value) {
    try {
      URI uri = URI.create(value);
      if (!uri.isAbsolute() || uri.getUserInfo() != null || uri.getHost() == null
          || !(uri.getScheme().equalsIgnoreCase("http") || uri.getScheme().equalsIgnoreCase("https"))) {
        throw new IllegalArgumentException();
      }
      return uri;
    } catch (RuntimeException error) {
      throw CollectionException.blocked(field + "_invalid");
    }
  }

  public static boolean sameOrigin(URI left, URI right) {
    return left.getScheme().equalsIgnoreCase(right.getScheme())
        && left.getHost().equalsIgnoreCase(right.getHost()) && port(left) == port(right);
  }

  public static String origin(URI uri) {
    int port = port(uri);
    boolean defaultPort = port == 80 && uri.getScheme().equalsIgnoreCase("http")
        || port == 443 && uri.getScheme().equalsIgnoreCase("https");
    return uri.getScheme().toLowerCase() + "://" + uri.getHost().toLowerCase()
        + (defaultPort ? "" : ":" + port);
  }

  private static int port(URI uri) {
    return uri.getPort() >= 0 ? uri.getPort() : (uri.getScheme().equalsIgnoreCase("https") ? 443 : 80);
  }

  private static boolean accessBarrier(URI uri) {
    String value = uri.toString().toLowerCase();
    return value.contains("/login") || value.contains("/signin") || value.contains("captcha");
  }

  private static boolean blank(String value) { return value == null || value.isBlank(); }
}
