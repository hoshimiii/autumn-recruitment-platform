package dev.recruitops;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zaxxer.hikari.HikariDataSource;
import dev.recruitops.config.SqliteConfig;
import dev.recruitops.domain.DiscoveredCandidate;
import dev.recruitops.domain.ReviewStatus;
import dev.recruitops.infrastructure.CandidateRepository;
import dev.recruitops.infrastructure.JsonValues;
import dev.recruitops.infrastructure.ReviewRepository;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.JdbcTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import static org.junit.jupiter.api.Assertions.*;

class SqliteConcurrencyTest {
  @TempDir Path directory;

  @Test void concurrentCreatesReturnTheirOwnGeneratedIds() throws Exception {
    try (HikariDataSource dataSource = database()) {
      JdbcTemplate jdbc = new JdbcTemplate(dataSource);
      source(jdbc);
      var mapper = new ObjectMapper();
      var json = new JsonValues(mapper);
      var manuals = transactional(new dev.recruitops.infrastructure.ManualItemRepository(jdbc, json), dataSource);
      var runs = transactional(new dev.recruitops.infrastructure.RunRepository(jdbc), dataSource);
      var sources = transactional(new dev.recruitops.infrastructure.SourceRepository(jdbc), dataSource);
      var template = sources.find(1).orElseThrow();
      assertEquals(8, race(() -> {
        String name = "Worker-" + java.util.UUID.randomUUID();
        var fields = new java.util.HashMap<>(json.map(template)); fields.put("name",name);
        var createdSource = sources.create(mapper.convertValue(fields, dev.recruitops.domain.Source.class), null);
        assertEquals(name, createdSource.name());
        var manual = manuals.create(new dev.recruitops.infrastructure.ManualItemRepository.ManualInput(
            "Fixture",name,null,java.util.List.of(),null,java.util.List.of(),null,"Review",false,false,null,null,0,"PENDING",null));
        assertEquals(name, manual.companyName());
        var run = runs.start(createdSource.id(), name);
        assertEquals(name, run.triggerType());
        return 1;
      }));
    }
  }

  @SuppressWarnings("unchecked")
  private <T> T transactional(T target, HikariDataSource dataSource) {
    var proxy = new org.springframework.aop.framework.ProxyFactory(target);
    proxy.setProxyTargetClass(true);
    proxy.addAdvice(new org.springframework.transaction.interceptor.TransactionInterceptor(
        new JdbcTransactionManager(dataSource),new org.springframework.transaction.annotation.AnnotationTransactionAttributeSource()));
    return (T) proxy.getProxy();
  }

  @Test void concurrentCandidateReplaysAreAtomicAndFileDatabaseUsesWal() throws Exception {
    try (HikariDataSource dataSource = database()) {
      JdbcTemplate jdbc = new JdbcTemplate(dataSource);
      assertEquals("wal", jdbc.queryForObject("PRAGMA journal_mode", String.class));
      assertTrue(dataSource.getMaximumPoolSize() > 1);
      assertTrue(jdbc.queryForObject("PRAGMA busy_timeout", Integer.class) >= 1000);
      source(jdbc);
      var json = new JsonValues(new ObjectMapper());
      var candidates = new CandidateRepository(jdbc, json);
      var item = new DiscoveredCandidate(Map.of("companyName", "Fixture"),
          (com.fasterxml.jackson.databind.node.ObjectNode) new ObjectMapper().readTree("{\"companyName\":\"Fixture\"}"), "hash", "identity", 1.0);
      assertEquals(1, race(() -> candidates.insertIfAbsent(1, 1, item)));
      assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM review_items", Integer.class));
    }
  }

  @Test void competingReviewDecisionsHaveExactlyOneWinner() throws Exception {
    try (HikariDataSource dataSource = database()) {
      JdbcTemplate jdbc = new JdbcTemplate(dataSource);
      source(jdbc);
      jdbc.update("INSERT INTO review_items(id,source_id,candidate_payload,normalized_payload,status) VALUES(1,1,'{}','{}','PENDING_REVIEW')");
      var reviews = new ReviewRepository(jdbc, new JsonValues(new ObjectMapper()));
      var transaction = new TransactionTemplate(new JdbcTransactionManager(dataSource));
      int winners = race(() -> transaction.execute(status -> {
        if (!reviews.claimPending(1)) return 0;
        reviews.transition(1, ReviewStatus.REJECTED, "test decision");
        return 1;
      }));
      assertEquals(1, winners);
      assertEquals("REJECTED", jdbc.queryForObject("SELECT status FROM review_items WHERE id=1", String.class));
      assertThrows(IllegalStateException.class, () -> reviews.transition(1, ReviewStatus.BLOCKED, "late decision"));
    }
  }

  private HikariDataSource database() {
    DataSourceProperties properties = new DataSourceProperties();
    properties.setUrl("jdbc:sqlite:" + directory.resolve("test.sqlite"));
    properties.setDriverClassName("org.sqlite.JDBC");
    HikariDataSource source = (HikariDataSource) new SqliteConfig().dataSource(properties);
    Flyway.configure().dataSource(source).load().migrate();
    return source;
  }

  private void source(JdbcTemplate jdbc) {
    jdbc.update("DELETE FROM sources");
    jdbc.update("INSERT INTO sources(id,name,type,base_url,adapter_type,mode,compliance_status,trust_level,scope) VALUES(1,'Fixture','COMPANY','https://example.org','JSON','JSON','ALLOWED','OFFICIAL','TEST')");
    jdbc.update("INSERT INTO collection_runs(id,source_id,trigger_type,status) VALUES(1,1,'TEST','SUCCESS')");
  }

  private int race(java.util.concurrent.Callable<Integer> action) throws Exception {
    var executor = Executors.newFixedThreadPool(8);
    var start = new CountDownLatch(1);
    var futures = new ArrayList<java.util.concurrent.Future<Integer>>();
    try {
      for (int index = 0; index < 8; index++) futures.add(executor.submit(() -> { start.await(); return action.call(); }));
      start.countDown();
      int total = 0;
      for (var future : futures) total += future.get(15, TimeUnit.SECONDS);
      return total;
    } finally { executor.shutdownNow(); }
  }
}
