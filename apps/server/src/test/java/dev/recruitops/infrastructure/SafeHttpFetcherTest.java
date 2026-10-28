package dev.recruitops.infrastructure;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.sun.net.httpserver.HttpServer;
import dev.recruitops.application.CollectionException;
import dev.recruitops.domain.Source;
import java.net.InetSocketAddress;
import java.net.InetAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SafeHttpFetcherTest {
  private HttpServer server;
  private ExecutorService handlers;
  private SafeHttpFetcher fetcher;
  private AddressPolicy addresses;
  private String origin;

  @BeforeEach void start() throws Exception {
    server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    handlers = Executors.newCachedThreadPool();
    server.setExecutor(handlers);
    server.start();
    origin = "http://collector.invalid:" + server.getAddress().getPort();
    // Explicit test-only policy: the synthetic public-source name maps to our owned fixture.
    addresses = mock(AddressPolicy.class);
    when(addresses.resolvePublic(any())).thenReturn(new InetAddress[]{InetAddress.getLoopbackAddress()});
    fetcher = new SafeHttpFetcher(mock(RequestPacer.class), addresses, "AutumnRecruitmentBot/1.0", 1);
  }

  @AfterEach void stop() throws Exception {
    server.stop(0);
    handlers.shutdownNow();
    assertTrue(handlers.awaitTermination(3, TimeUnit.SECONDS));
    fetcher.close();
  }

  @Test void connectsOnlyToVettedSnapshotAndPreservesHostAndHeaders() throws Exception {
    AtomicReference<String> host = new AtomicReference<>();
    server.createContext("/jobs", exchange -> {
      host.set(exchange.getRequestHeaders().getFirst("Host"));
      assertEquals("AutumnRecruitmentBot/1.0", exchange.getRequestHeaders().getFirst("User-Agent"));
      assertEquals("fixture-etag", exchange.getRequestHeaders().getFirst("If-None-Match"));
      byte[] bytes = "fixture".getBytes(StandardCharsets.UTF_8);
      exchange.sendResponseHeaders(200, bytes.length);
      exchange.getResponseBody().write(bytes); exchange.close();
    });
    when(addresses.resolvePublic(any())).thenReturn(new InetAddress[]{InetAddress.getLoopbackAddress()},
        new InetAddress[]{InetAddress.getByName("192.168.1.1")});
    var result = fetcher.fetch(source(1), URI.create(origin + "/jobs"), true, 0, false);
    assertEquals("fixture", new String(result.body(), StandardCharsets.UTF_8));
    assertEquals("collector.invalid:" + server.getAddress().getPort(), host.get());
    verify(addresses, times(1)).resolvePublic(any());
  }

  @Test void deadlineClosesHeaderOnlyBodyAndPeerConnection() throws Exception {
    CountDownLatch peerClosed = new CountDownLatch(1);
    server.createContext("/stall", exchange -> {
      exchange.sendResponseHeaders(200, 0); exchange.getResponseBody().flush();
      try {
        Thread.sleep(1600);
        for (int i = 0; i < 100; i++) { exchange.getResponseBody().write(new byte[8192]); exchange.getResponseBody().flush(); }
      } catch (java.io.IOException expected) { peerClosed.countDown(); }
      catch (InterruptedException error) { Thread.currentThread().interrupt(); }
      finally { exchange.close(); }
    });
    // A numeric host also exercises the transport's literal-address path.
    origin = "http://127.0.0.1:" + server.getAddress().getPort();
    assertTimeoutPreemptively(Duration.ofMillis(2400), () -> {
      CollectionException error = assertThrows(CollectionException.class,
          () -> fetcher.fetch(source(1), URI.create(origin + "/stall"), false, 0, false));
      assertEquals("FAILED", error.status());
    });
    assertTrue(peerClosed.await(3, TimeUnit.SECONDS), "deadline must actively close the socket");
  }

  @Test void deadlineAlsoStopsContinuousChunkDrip() {
    server.createContext("/drip", exchange -> {
      exchange.sendResponseHeaders(200, 0);
      try {
        for (int i = 0; i < 50; i++) {
          exchange.getResponseBody().write('x'); exchange.getResponseBody().flush(); Thread.sleep(100);
        }
      } catch (java.io.IOException expected) { }
      catch (InterruptedException error) { Thread.currentThread().interrupt(); }
      finally { exchange.close(); }
    });
    origin = "http://127.0.0.1:" + server.getAddress().getPort();
    assertTimeoutPreemptively(Duration.ofMillis(2400), () -> assertThrows(CollectionException.class,
        () -> fetcher.fetch(source(1), URI.create(origin + "/drip"), false, 0, false)));
  }

  @Test void oversizedChunkedBodyIsNotDrained() {
    server.createContext("/large", exchange -> {
      exchange.sendResponseHeaders(200, 0);
      try {
        exchange.getResponseBody().write(new byte[SafeHttpFetcher.MAX_BODY_BYTES + 1]);
        exchange.getResponseBody().flush(); Thread.sleep(5000);
      } catch (java.io.IOException expected) { }
      catch (InterruptedException error) { Thread.currentThread().interrupt(); }
      finally { exchange.close(); }
    });
    origin = "http://127.0.0.1:" + server.getAddress().getPort();
    assertTimeoutPreemptively(Duration.ofSeconds(2), () -> {
      var error = assertThrows(CollectionException.class,
          () -> fetcher.fetch(source(5), URI.create(origin + "/large"), false, 0, false));
      assertEquals("response_body_too_large", error.getMessage());
    });
  }

  @Test void redirectRevalidatesSnapshotBeforeTargetDispatch() {
    var targetRequests = new java.util.concurrent.atomic.AtomicInteger();
    server.createContext("/redirect", exchange -> {
      exchange.getResponseHeaders().set("Location", "/target");
      exchange.sendResponseHeaders(302, 0); exchange.getResponseBody().flush();
      // A redirect body need not finish before its next-hop policy can be checked.
      try { Thread.sleep(3000); } catch (InterruptedException error) { Thread.currentThread().interrupt(); }
      finally { exchange.close(); }
    });
    server.createContext("/target", exchange -> { targetRequests.incrementAndGet(); exchange.close(); });
    when(addresses.resolvePublic(any())).thenReturn(new InetAddress[]{InetAddress.getLoopbackAddress()})
        .thenThrow(CollectionException.blocked("request_address_not_allowed"));
    assertTimeoutPreemptively(Duration.ofSeconds(2), () -> {
      var error = assertThrows(CollectionException.class,
          () -> fetcher.fetch(source(5), URI.create(origin + "/redirect"), false, 0, false));
      assertEquals("BLOCKED", error.status());
    });
    assertEquals(0, targetRequests.get());
    verify(addresses, times(2)).resolvePublic(any());
  }

  @Test void completedDeadlinesAreRemovedAndLifecycleStopsTimer() throws Exception {
    server.createContext("/empty", exchange -> { exchange.sendResponseHeaders(204, -1); exchange.close(); });
    for (int i = 0; i < 5; i++) {
      assertEquals(204, fetcher.fetch(source(30), URI.create(origin + "/empty"), false, 0, false).status());
    }
    var field = SafeHttpFetcher.class.getDeclaredField("deadlines"); field.setAccessible(true);
    var timer = (ScheduledThreadPoolExecutor) field.get(fetcher);
    assertEquals(0, timer.getQueue().size());
    fetcher.close();
    assertTrue(timer.awaitTermination(2, TimeUnit.SECONDS));
  }

  @Test void closingFetcherCancelsAnActiveBodyBeforeStoppingTimer() throws Exception {
    CountDownLatch headers = new CountDownLatch(1);
    server.createContext("/shutdown", exchange -> {
      exchange.sendResponseHeaders(200, 0); headers.countDown();
      try {
        for (int i = 0; i < 50; i++) {
          exchange.getResponseBody().write('x'); exchange.getResponseBody().flush(); Thread.sleep(100);
        }
      } catch (java.io.IOException expected) { }
      catch (InterruptedException error) { Thread.currentThread().interrupt(); }
      finally { exchange.close(); }
    });
    ExecutorService caller = Executors.newSingleThreadExecutor();
    try {
      var request = caller.submit(() -> assertThrows(CollectionException.class,
          () -> fetcher.fetch(source(30), URI.create(origin + "/shutdown"), false, 0, false)));
      assertTrue(headers.await(2, TimeUnit.SECONDS));
      fetcher.close();
      assertEquals("FAILED", request.get(2, TimeUnit.SECONDS).status());
    } finally {
      server.stop(0); caller.shutdownNow(); assertTrue(caller.awaitTermination(2, TimeUnit.SECONDS));
    }
  }

  private Source source(int timeout) {
    return new Source(1, "Fixture", "COMPANY", origin, origin + "/jobs", "JSON", "JSON",
        "ALLOWED", "OFFICIAL", "INTERNET_BIG_TECH", true, null, null, 0, 1, 1, "{}", "{}",
        origin + "/terms", "2026-07-25T00:00:00Z", origin + "/robots.txt", timeout,
        "fixture-etag", null, null, null, false, null, null, 1);
  }
}
