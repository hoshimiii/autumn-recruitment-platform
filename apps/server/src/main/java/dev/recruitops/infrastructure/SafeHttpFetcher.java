package dev.recruitops.infrastructure;

import dev.recruitops.application.CollectionException;
import dev.recruitops.application.SourcePolicy;
import dev.recruitops.domain.Source;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.apache.hc.client5.http.DnsResolver;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.ssl.DefaultClientTlsStrategy;
import org.apache.hc.client5.http.ssl.TlsSocketStrategy;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.io.CloseMode;
import org.apache.hc.core5.util.Timeout;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class SafeHttpFetcher implements AutoCloseable {
  static final int MAX_BODY_BYTES = 2 * 1024 * 1024;
  private final long connectTimeoutSeconds;
  private final ScheduledThreadPoolExecutor deadlines;
  private final Set<HttpGet> activeRequests = ConcurrentHashMap.newKeySet();
  private final TlsSocketStrategy tls;
  private final RequestPacer pacer;
  private final AddressPolicy addresses;
  private final String userAgent;

  @org.springframework.beans.factory.annotation.Autowired
  public SafeHttpFetcher(
      RequestPacer pacer,
      AddressPolicy addresses,
      @Value("${recruitops.collector.user-agent:AutumnRecruitmentBot/1.0}") String userAgent,
      @Value("${recruitops.collector.connect-timeout-seconds:10}") long connectTimeoutSeconds) {
    this(pacer, addresses, userAgent, connectTimeoutSeconds, DefaultClientTlsStrategy.createDefault());
  }

  SafeHttpFetcher(RequestPacer pacer, AddressPolicy addresses, String userAgent,
                  long connectTimeoutSeconds, TlsSocketStrategy tls) {
    this.pacer = pacer;
    this.addresses = addresses;
    this.userAgent = userAgent;
    this.connectTimeoutSeconds = Math.max(1, connectTimeoutSeconds);
    this.tls = tls;
    this.deadlines = new ScheduledThreadPoolExecutor(1, task -> {
      Thread thread = new Thread(task, "collector-deadline");
      thread.setDaemon(true);
      return thread;
    });
    deadlines.setRemoveOnCancelPolicy(true);
  }

  public FetchResult fetch(Source source, URI initialUri, boolean conditional,
                           long robotsDelayMillis, boolean detectBarrier) {
    URI base = SourcePolicy.valid("base_url", source.baseUrl());
    URI current = initialUri;
    int redirects = 0;
    while (true) {
      requireRequestUri(base, current);
      InetAddress[] approvedAddresses = addresses.resolvePublic(current);
      pacer.await(source, current, robotsDelayMillis);
      Response response = send(source, current, approvedAddresses, conditional && redirects == 0);
      int status = response.status();
      if (isRedirect(status)) {
        if (redirects >= 3) throw CollectionException.blocked("redirect_limit_exceeded");
        String location = response.location();
        if (location == null) throw CollectionException.blocked("redirect_location_missing");
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
        throw CollectionException.blocked("http_" + status);
      }
      byte[] body = response.body();
      if (detectBarrier && status >= 200 && status < 300
          && accessBarrier(new String(body, java.nio.charset.StandardCharsets.UTF_8))) {
        throw CollectionException.blocked("login_or_captcha_content");
      }
      return new FetchResult(status, current, body,
          response.contentType(), response.etag(), response.lastModified());
    }
  }

  private Response send(Source source, URI uri, InetAddress[] approved, boolean conditional) {
    if (Thread.currentThread().isInterrupted()) throw CollectionException.failed("request_interrupted");
    HttpGet request = new HttpGet(uri);
    request.setHeader("User-Agent", userAgent);
    request.setHeader("Accept", "*/*");
    var manager = PoolingHttpClientConnectionManagerBuilder.create()
        .setDnsResolver(pinnedResolver(uri, approved))
        .setTlsSocketStrategy(tls)
        .setMaxConnTotal(1).setMaxConnPerRoute(1)
        .setDefaultConnectionConfig(ConnectionConfig.custom()
            .setConnectTimeout(Timeout.ofSeconds(connectTimeoutSeconds)).build()).build();
    try (var client = HttpClients.custom().setConnectionManager(manager)
        .disableRedirectHandling().disableAutomaticRetries().disableCookieManagement()
        .disableContentCompression().build()) {
      request.setConfig(RequestConfig.custom()
          .setResponseTimeout(Timeout.ofSeconds(Math.max(1, source.timeoutSeconds()))).build());
      if (conditional && source.etag() != null && !source.etag().isBlank()) {
        request.setHeader("If-None-Match", source.etag());
      }
      if (conditional && source.lastModified() != null && !source.lastModified().isBlank()) {
        request.setHeader("If-Modified-Since", source.lastModified());
      }
      java.util.concurrent.ScheduledFuture<?> deadline;
      synchronized (deadlines) {
        if (deadlines.isShutdown()) throw CollectionException.failed("request_failed");
        activeRequests.add(request);
        deadline = deadlines.schedule(request::cancel, Math.max(1, source.timeoutSeconds()), TimeUnit.SECONDS);
      }
      ClassicHttpResponse response = null;
      try {
        response = client.executeOpen(null, request, null);
        int status = response.getCode();
        byte[] body = isRedirect(status) || status == 401 || status == 403 || status == 429
            ? new byte[0] : readBounded(response);
        if (request.isCancelled()) throw CollectionException.failed("request_failed");
        return new Response(status, body, header(response, "Location"),
            header(response, "Content-Type"), header(response, "ETag"), header(response, "Last-Modified"));
      } finally {
        deadline.cancel(false);
        // Close the socket before the entity: entity.close() may otherwise drain an untrusted body.
        client.close(CloseMode.IMMEDIATE);
        if (response != null) {
          try { response.close(); } catch (IOException ignored) { /* Socket already closed intentionally. */ }
        }
        activeRequests.remove(request);
      }
    } catch (IOException | IllegalArgumentException error) {
      throw CollectionException.failed("request_failed", error);
    }
  }

  private byte[] readBounded(ClassicHttpResponse response) {
    if (response.getEntity() == null) return new byte[0];
    long declared = response.getEntity().getContentLength();
    if (declared > MAX_BODY_BYTES) {
      throw CollectionException.failed("response_body_too_large");
    }
    try {
      InputStream input = response.getEntity().getContent();
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

  private static DnsResolver pinnedResolver(URI uri, InetAddress[] approved) {
    InetAddress[] snapshot = approved.clone();
    return new DnsResolver() {
      public InetAddress[] resolve(String host) throws UnknownHostException {
        if (!host.replace("[", "").replace("]", "").equalsIgnoreCase(
            uri.getHost().replace("[", "").replace("]", ""))) {
          throw new UnknownHostException("Unapproved transport host");
        }
        return snapshot.clone();
      }
      public String resolveCanonicalHostname(String host) throws UnknownHostException {
        resolve(host);
        return host;
      }
    };
  }

  private static String header(ClassicHttpResponse response, String name) {
    var value = response.getFirstHeader(name);
    return value == null ? ("Content-Type".equals(name) ? "application/octet-stream" : null) : value.getValue();
  }

  private record Response(int status, byte[] body, String location, String contentType,
                          String etag, String lastModified) { }

  @Override @jakarta.annotation.PreDestroy public void close() {
    synchronized (deadlines) {
      activeRequests.forEach(HttpGet::cancel);
      deadlines.shutdownNow();
    }
  }
}
