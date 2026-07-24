package dev.recruitops;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import dev.recruitops.application.CollectionWorkflow;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

abstract class CollectorTestSupport {
  @Autowired JdbcTemplate jdbc;
  @Autowired CollectionWorkflow workflow;
  HttpServer server;
  String origin;
  private final AtomicInteger sourceSequence = new AtomicInteger();

  @BeforeEach
  void startFixtureServer() throws Exception {
    clearDatabase();
    server = HttpServer.create(new InetSocketAddress(InetAddress.getByName("127.0.0.1"), 0), 0);
    server.start();
    origin = "http://127.0.0.1:" + server.getAddress().getPort();
  }

  @AfterEach
  void stopFixtureServer() {
    if (server != null) server.stop(0);
  }

  void clearDatabase() {
    jdbc.update("DELETE FROM audit_logs");
    jdbc.update("DELETE FROM job_snapshots");
    jdbc.update("DELETE FROM review_items");
    jdbc.update("DELETE FROM jobs");
    jdbc.update("DELETE FROM collection_runs");
    jdbc.update("DELETE FROM sources");
  }

  long insertSource(String path, String mode, boolean enabled, String compliance,
                    String termsConfirmedAt, int maxItems, String selectors, String mapping) {
    String listUrl = path.startsWith("http") ? path : origin + path;
    jdbc.update("""
        INSERT INTO sources (name,type,base_url,list_url,adapter_type,mode,compliance_status,trust_level,
        scope,enabled,min_interval_seconds,max_pages,max_items,selectors_json,field_mapping_json,
        terms_confirmed_at,robots_url,timeout_seconds)
        VALUES (?,'COMPANY',?,?,?, ?,?,'OFFICIAL','INTERNET_BIG_TECH',?,0,1,?,?,?,?,
        ?,3)
        """, "Fixture " + sourceSequence.incrementAndGet(), origin, listUrl, mode, mode, compliance,
        enabled, maxItems, selectors, mapping, termsConfirmedAt, origin + "/robots.txt");
    return jdbc.queryForObject("SELECT last_insert_rowid()", Long.class);
  }

  void route(String path, int status, String contentType, Supplier<String> body,
             AtomicInteger requests) {
    server.createContext(path, exchange -> {
      if (requests != null) requests.incrementAndGet();
      respond(exchange, status, contentType, body.get());
    });
  }

  void route(String path, HttpHandler handler) {
    server.createContext(path, handler);
  }

  static void respond(HttpExchange exchange, int status, String contentType, String body)
      throws IOException {
    byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
    if (contentType != null) exchange.getResponseHeaders().set("Content-Type", contentType);
    exchange.sendResponseHeaders(status, status == 304 ? -1 : bytes.length);
    if (status != 304) exchange.getResponseBody().write(bytes);
    exchange.close();
  }

  int count(String table) {
    return jdbc.queryForObject("SELECT COUNT(*) FROM " + table, Integer.class);
  }

  static String mapping() {
    return """
        {"companyName":"$const:Acme","title":"title","city":"city","jobFamily":"family",
        "jobType":"$const:FULL_TIME","deadline":"deadline","techStack":"techStack",
        "certificateRequirements":"certificates","jd":"jd","requirements":"requirements",
        "sourceTrust":"$const:OFFICIAL","sourceUrlTemplate":"%s/jobs/{id}",
        "applyUrl":"applyUrl","priority":"$const:2","applyMethod":"$const:PORTAL"}
        """;
  }
}
