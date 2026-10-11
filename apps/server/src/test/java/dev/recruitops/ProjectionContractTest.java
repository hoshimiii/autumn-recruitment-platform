package dev.recruitops;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import dev.recruitops.infrastructure.ElasticJobIndex;
import dev.recruitops.infrastructure.RedisJobCache;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ProjectionContractTest {
  private dev.recruitops.infrastructure.DatasetNamespace namespace() {
    var properties=new org.springframework.boot.autoconfigure.jdbc.DataSourceProperties();properties.setUrl("jdbc:sqlite:file:projection-test?mode=memory&cache=shared");
    return new dev.recruitops.infrastructure.DatasetNamespace(properties);
  }
  @Test void middlewareNamespaceDistinguishesIndependentDatabases() {
    var first=new org.springframework.boot.autoconfigure.jdbc.DataSourceProperties();first.setUrl("jdbc:sqlite:./first.sqlite");
    var second=new org.springframework.boot.autoconfigure.jdbc.DataSourceProperties();second.setUrl("jdbc:sqlite:./second.sqlite");
    assertNotEquals(new dev.recruitops.infrastructure.DatasetNamespace(first).value(),new dev.recruitops.infrastructure.DatasetNamespace(second).value());
    var same=new org.springframework.boot.autoconfigure.jdbc.DataSourceProperties();same.setUrl("jdbc:sqlite:"+java.nio.file.Path.of("first.sqlite").toAbsolutePath());
    assertEquals(new dev.recruitops.infrastructure.DatasetNamespace(first).value(),new dev.recruitops.infrastructure.DatasetNamespace(same).value());
  }

  @Test void failedRefreshBatchReplaysSupersededEventsAndAcknowledgesAfterRefresh() throws Exception {
    var mapper=new ObjectMapper();var version=new java.util.concurrent.atomic.AtomicLong();
    var refreshes=new java.util.concurrent.atomic.AtomicInteger();
    var server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
    server.createContext("/",exchange->{
      String path=exchange.getRequestURI().getPath();int code=200;String body="{}";
      if(path.contains("/_doc/")) {
        long requested=Long.parseLong(exchange.getRequestURI().getQuery().split("&")[0].split("=")[1]);
        if(requested<version.get()) {code=409;body="{\"error\":{\"type\":\"version_conflict_engine_exception\"}}";}
        else version.set(requested);
      } else if(path.endsWith("/_refresh") && refreshes.incrementAndGet()==1) {code=503;}
      exchange.getRequestBody().readAllBytes();byte[] bytes=body.getBytes(StandardCharsets.UTF_8);
      exchange.sendResponseHeaders(code,bytes.length);exchange.getResponseBody().write(bytes);exchange.close();
    });server.start();
    try {
      var index=new ElasticJobIndex(mapper,"http://127.0.0.1:"+server.getAddress().getPort(),true,namespace());
      var jobs=mock(dev.recruitops.infrastructure.JobRepository.class);
      var outbox=mock(dev.recruitops.infrastructure.ProjectionOutbox.class);
      var job=mapper.readValue("{\"id\":1,\"status\":\"PUBLISHED\",\"companyName\":\"Fixture\",\"title\":\"Java\",\"techStack\":[],\"certificateRequirements\":[]}",dev.recruitops.domain.Job.class);
      when(jobs.find(1)).thenReturn(java.util.Optional.of(job));
      when(outbox.next()).thenReturn(List.of(new dev.recruitops.infrastructure.ProjectionOutbox.Event(10,1),new dev.recruitops.infrastructure.ProjectionOutbox.Event(11,1)));
      var service=new dev.recruitops.application.JobProjectionService(index,jobs,outbox);
      service.synchronize();verify(outbox,never()).done(anyLong());assertEquals(11,version.get());
      service.synchronize();verify(outbox).done(10);verify(outbox).done(11);
    } finally {server.stop(0);}
  }

  @Test void fuzzySearchUsesFixedQueryAndBoundedIdsAndFailsClosedOnOversizedMatches() throws Exception {
    var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    var captured = new java.util.concurrent.atomic.AtomicReference<String>();
    var response = new java.util.concurrent.atomic.AtomicReference<>("{\"hits\":{\"total\":{\"value\":2,\"relation\":\"eq\"},\"hits\":[{\"_id\":\"2\"},{\"_id\":\"7\"}]}}");
    server.createContext("/", exchange -> {
      captured.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
      byte[] body = response.get().getBytes(StandardCharsets.UTF_8);
      exchange.sendResponseHeaders(200, body.length);
      exchange.getResponseBody().write(body);
      exchange.close();
    });
    server.start();
    try {
      var index = new ElasticJobIndex(new ObjectMapper(), "http://127.0.0.1:" + server.getAddress().getPort(), true,namespace());
      assertEquals(List.of(2L, 7L), index.searchIds("Jvaa").orElseThrow());
      var query = new ObjectMapper().readTree(captured.get());
      assertEquals("AUTO", query.at("/query/multi_match/fuzziness").asText());
      assertEquals(1001, query.path("size").asInt());
      response.set("{\"hits\":{\"total\":{\"value\":1002,\"relation\":\"eq\"},\"hits\":[]}}");
      assertTrue(index.searchIds("Java").isEmpty());
    } finally { server.stop(0); }
    var offline = new ElasticJobIndex(new ObjectMapper(), "http://127.0.0.1:" + server.getAddress().getPort(), true,namespace());
    assertTrue(offline.searchIds("Java").isEmpty());
  }

  @Test void middlewareConfigurationRejectsRemoteOrCredentialBearingEndpoints() {
    assertThrows(IllegalArgumentException.class, () -> new ElasticJobIndex(new ObjectMapper(), "https://example.org", true,namespace()));
    assertThrows(IllegalArgumentException.class, () -> new ElasticJobIndex(new ObjectMapper(), "http://user:secret@localhost:9200", true,namespace()));
  }

  @Test void redisFailureIsCacheMissAndNeverSuppressesAuthoritativeSql() {
    StringRedisTemplate redis = mock(StringRedisTemplate.class);
    @SuppressWarnings("unchecked") ValueOperations<String,String> values = mock(ValueOperations.class);
    when(redis.opsForValue()).thenReturn(values);
    when(values.get("key")).thenThrow(new IllegalStateException("offline"));
    var cache = new RedisJobCache(redis, true);
    assertTrue(cache.get("key").isEmpty());
    assertDoesNotThrow(() -> cache.put("key", "value"));
    var disabled = new RedisJobCache(redis, false);
    assertTrue(disabled.get("other").isEmpty());
    verify(values, never()).get("other");
  }
}
