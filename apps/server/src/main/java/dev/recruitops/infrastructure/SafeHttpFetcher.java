package dev.recruitops.infrastructure;

import dev.recruitops.application.CollectionException;
import dev.recruitops.application.SourcePolicy;
import dev.recruitops.domain.Source;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class SafeHttpFetcher {
  static final int MAX_BODY_BYTES = 2 * 1024 * 1024;
  private final HttpClient client;
  private final RequestPacer pacer;
  private final String userAgent;

  public SafeHttpFetcher(
      RequestPacer pacer,
      @Value("${recruitops.collector.user-agent:AutumnRecruitmentBot/1.0}") String userAgent,
      @Value("${recruitops.collector.connect-timeout-seconds:10}") long connectTimeoutSeconds) {
    this.pacer = pacer;
    this.userAgent = userAgent;
    this.client = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(Math.max(1, connectTimeoutSeconds)))
        .followRedirects(HttpClient.Redirect.NEVER)
        .build();
  }

  public FetchResult fetch(Source source, URI initialUri, boolean conditional,
                           long robotsDelayMillis, boolean detectBarrier) {
    URI base = SourcePolicy.valid("base_url", source.baseUrl());
    URI current = initialUri;
    int redirects = 0;
    while (true) {
      requireRequestUri(base, current);
      pacer.await(source, current, robotsDelayMillis);
      HttpResponse<InputStream> response = send(source, current, conditional && redirects == 0);
      int status = response.statusCode();
      if (isRedirect(status)) {
        close(response.body());
        if (redirects >= 3) throw CollectionException.blocked("redirect_limit_exceeded");
        String location = response.headers().firstValue("Location")
            .orElseThrow(() -> CollectionException.blocked("redirect_location_missing"));
        URI next;
        try { next = current.resolve(location); }
        catch (RuntimeException error) { throw CollectionException.blocked("redirect_url_invalid"); }
        if (!SourcePolicy.sameOrigin(base, next)) {
          throw CollectionException.blocked("redirect_origin_not_allowed");
        }
        if (accessBarrier(next.toString())) throw CollectionException.blocked("login_or_captcha_redirect");
        current = next;
        redirects++;
        continue;
      }
      if (status == 401 || status == 403 || status == 429) {
        close(response.body());
        throw CollectionException.blocked("http_" + status);
      }
      byte[] body = readBounded(response);
      if (detectBarrier && status >= 200 && status < 300
          && accessBarrier(new String(body, java.nio.charset.StandardCharsets.UTF_8))) {
        throw CollectionException.blocked("login_or_captcha_content");
      }
      return new FetchResult(status, current, body,
          response.headers().firstValue("Content-Type").orElse("application/octet-stream"),
          response.headers().firstValue("ETag").orElse(null),
          response.headers().firstValue("Last-Modified").orElse(null));
    }
  }

  private HttpResponse<InputStream> send(Source source, URI uri, boolean conditional) {
    try {
      HttpRequest.Builder builder = HttpRequest.newBuilder(uri)
          .timeout(Duration.ofSeconds(Math.max(1, source.timeoutSeconds())))
          .header("User-Agent", userAgent)
          .header("Accept", "*/*")
          .GET();
      if (conditional && source.etag() != null && !source.etag().isBlank()) {
        builder.header("If-None-Match", source.etag());
      }
      if (conditional && source.lastModified() != null && !source.lastModified().isBlank()) {
        builder.header("If-Modified-Since", source.lastModified());
      }
      return client.send(builder.build(), HttpResponse.BodyHandlers.ofInputStream());
    } catch (InterruptedException error) {
      Thread.currentThread().interrupt();
      throw CollectionException.failed("request_interrupted", error);
    } catch (IOException | IllegalArgumentException error) {
      throw CollectionException.failed("request_failed", error);
    }
  }

  private byte[] readBounded(HttpResponse<InputStream> response) {
    long declared = response.headers().firstValueAsLong("Content-Length").orElse(-1);
    if (declared > MAX_BODY_BYTES) {
      close(response.body());
      throw CollectionException.failed("response_body_too_large");
    }
    try (InputStream input = response.body()) {
      byte[] body = input.readNBytes(MAX_BODY_BYTES + 1);
      if (body.length > MAX_BODY_BYTES) throw CollectionException.failed("response_body_too_large");
      return body;
    } catch (IOException error) {
      throw CollectionException.failed("response_read_failed", error);
    }
  }

  private static void requireRequestUri(URI base, URI uri) {
    SourcePolicy.valid("request_url", uri.toString());
    if (!SourcePolicy.sameOrigin(base, uri)) {
      throw CollectionException.blocked("request_origin_not_allowed");
    }
    if (accessBarrier(uri.toString())) throw CollectionException.blocked("login_or_captcha_url");
  }

  private static boolean accessBarrier(String value) {
    String lower = value.toLowerCase(Locale.ROOT);
    return lower.contains("/login") || lower.contains("/signin") || lower.contains("captcha")
        || lower.contains("<title>login") || lower.contains("login required")
        || lower.contains("验证码") || lower.contains("登录验证");
  }

  private static boolean isRedirect(int status) {
    return status == 301 || status == 302 || status == 303 || status == 307 || status == 308;
  }

  private static void close(InputStream input) {
    try { input.close(); } catch (IOException ignored) { }
  }
}
