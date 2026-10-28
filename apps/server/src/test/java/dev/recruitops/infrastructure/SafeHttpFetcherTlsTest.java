package dev.recruitops.infrastructure;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.sun.net.httpserver.*;
import dev.recruitops.application.CollectionException;
import dev.recruitops.domain.Source;
import java.net.*;
import java.nio.file.*;
import java.security.KeyStore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.*;
import javax.net.ssl.*;
import org.apache.hc.client5.http.ssl.DefaultClientTlsStrategy;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

class SafeHttpFetcherTlsTest {
  @TempDir static Path temporary;
  private static SSLContext serverContext;
  private static SSLContext trustedContext;
  private HttpsServer server;
  private final AtomicInteger requests = new AtomicInteger();
  private final AtomicReference<String> host = new AtomicReference<>();
  private final AtomicReference<String> sni = new AtomicReference<>();
  private AddressPolicy policy;

  @BeforeAll static void certificate() throws Exception {
    Path keytool = Path.of(System.getProperty("java.home"), "bin",
        System.getProperty("os.name").startsWith("Windows") ? "keytool.exe" : "keytool");
    Path store = temporary.resolve("fixture.p12");
    var process = new ProcessBuilder(keytool.toString(), "-genkeypair", "-alias", "fixture",
        "-keyalg", "RSA", "-keysize", "2048", "-validity", "2", "-dname", "CN=collector.invalid",
        "-ext", "SAN=dns:collector.invalid", "-storetype", "PKCS12", "-keystore", store.toString(),
        "-storepass", "fixture-password", "-noprompt").redirectErrorStream(true)
        .redirectOutput(ProcessBuilder.Redirect.DISCARD).start();
    assertTrue(process.waitFor(20, TimeUnit.SECONDS));
    assertEquals(0, process.exitValue());
    KeyStore keys = KeyStore.getInstance("PKCS12");
    try (var input = Files.newInputStream(store)) { keys.load(input, "fixture-password".toCharArray()); }
    var keyManagers = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
    keyManagers.init(keys, "fixture-password".toCharArray());
    serverContext = SSLContext.getInstance("TLS"); serverContext.init(keyManagers.getKeyManagers(), null, null);
    var trustManagers = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
    trustManagers.init(keys);
    trustedContext = SSLContext.getInstance("TLS"); trustedContext.init(null, trustManagers.getTrustManagers(), null);
  }

  @BeforeEach void start() throws Exception {
    server = HttpsServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    server.setHttpsConfigurator(new HttpsConfigurator(serverContext));
    server.createContext("/jobs", exchange -> {
      requests.incrementAndGet(); host.set(exchange.getRequestHeaders().getFirst("Host"));
      var session = (ExtendedSSLSession) ((HttpsExchange) exchange).getSSLSession();
      session.getRequestedServerNames().stream().filter(name -> name instanceof SNIHostName).findFirst()
          .ifPresent(name -> sni.set(((SNIHostName) name).getAsciiName()));
      exchange.sendResponseHeaders(200, 2); exchange.getResponseBody().write(new byte[]{'o', 'k'}); exchange.close();
    });
    server.start(); policy = mock(AddressPolicy.class);
    // Only this test policy maps a synthetic name to the owned TLS fixture.
    when(policy.resolvePublic(any())).thenReturn(new InetAddress[]{InetAddress.getByName("127.0.0.1")});
  }

  @AfterEach void stop() { server.stop(0); }

  @Test void pinnedHttpsPreservesTrustedHostnameHostAndSni() {
    String origin = origin("collector.invalid");
    try (var fetcher = trustedFetcher()) {
      assertArrayEquals(new byte[]{'o', 'k'}, fetcher.fetch(source(origin), URI.create(origin + "/jobs"), false, 0, false).body());
    }
    assertEquals("collector.invalid:" + server.getAddress().getPort(), host.get());
    assertEquals("collector.invalid", sni.get()); assertEquals(1, requests.get());
  }

  @Test void trustedCertificateForWrongHostnameIsRejectedBeforeHttp() {
    String origin = origin("other.invalid");
    try (var fetcher = trustedFetcher()) {
      assertThrows(CollectionException.class,
          () -> fetcher.fetch(source(origin), URI.create(origin + "/jobs"), false, 0, false));
    }
    assertEquals(0, requests.get());
  }

  @Test void defaultTrustRejectsSelfSignedFixtureBeforeHttp() {
    String origin = origin("collector.invalid");
    try (var fetcher = new SafeHttpFetcher(mock(RequestPacer.class), policy, "AutumnRecruitmentBot/1.0", 2)) {
      assertThrows(CollectionException.class,
          () -> fetcher.fetch(source(origin), URI.create(origin + "/jobs"), false, 0, false));
    }
    assertEquals(0, requests.get());
  }

  private SafeHttpFetcher trustedFetcher() {
    return new SafeHttpFetcher(mock(RequestPacer.class), policy, "AutumnRecruitmentBot/1.0", 2,
        new DefaultClientTlsStrategy(trustedContext));
  }
  private String origin(String name) { return "https://" + name + ":" + server.getAddress().getPort(); }
  private Source source(String origin) {
    return new Source(1, "TLS fixture", "COMPANY", origin, origin + "/jobs", "JSON", "JSON",
        "ALLOWED", "OFFICIAL", "INTERNET_BIG_TECH", true, null, null, 0, 1, 1, "{}", "{}",
        origin + "/terms", "2026-07-25T00:00:00Z", origin + "/robots.txt", 3,
        null, null, null, null, false, null, null, 1);
  }
}
