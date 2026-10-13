package dev.recruitops.infrastructure;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.nio.file.Path;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.env.MockEnvironment;
import org.sqlite.SQLiteDataSource;

class RequestPacerTest {
  @TempDir Path temporary;

  private JdbcTemplate database() {
    var source = new SQLiteDataSource();
    source.setUrl("jdbc:sqlite:" + temporary.resolve("pacing.sqlite"));
    source.setBusyTimeout(5000);
    var jdbc = new JdbcTemplate(source);
    jdbc.execute("CREATE TABLE origin_request_pacing(origin TEXT PRIMARY KEY,last_read_at INTEGER NOT NULL,interval_millis INTEGER NOT NULL)");
    return jdbc;
  }

  private RequestPacer pacer(JdbcTemplate jdbc) {
    var environment = new MockEnvironment();
    environment.setActiveProfiles("test");
    return new RequestPacer(0, environment, jdbc);
  }

  @Test void normalizedOriginSharesSlotsButDifferentOriginsAreIndependent() {
    var jdbc = database(); var pacer = pacer(jdbc);
    long first = pacer.reserve(URI.create("HTTPS://EXAMPLE.COM:443/a"), 10_000);
    long same = pacer.reserve(URI.create("https://example.com/b"), 10_000);
    long other = pacer.reserve(URI.create("https://other.example/b"), 10_000);
    assertTrue(same - first >= 10_000);
    assertTrue(other < same, "Another origin must not join the existing queue");
    assertEquals(2, jdbc.queryForObject("SELECT COUNT(*) FROM origin_request_pacing", Integer.class));
  }

  @Test void adjacentSourcesHonorBothIntervals() {
    var pacer = pacer(database()); var uri = URI.create("https://example.com");
    long first = pacer.reserve(uri, 10_000);
    long shorter = pacer.reserve(uri, 100);
    long longer = pacer.reserve(uri, 20_000);
    assertTrue(shorter - first >= 10_000);
    assertTrue(longer - shorter >= 20_000);
  }

  @Test void blockedReservationSamplesTimeAfterAcquiringDatabaseWriteLock() throws Exception {
    var jdbc = database(); var pacer = pacer(jdbc); var uri = URI.create("https://example.com");
    jdbc.update("INSERT INTO origin_request_pacing VALUES (?,?,?)", uri.toString(), System.currentTimeMillis()-1000, 100);
    var executor = Executors.newSingleThreadExecutor();
    try (var blocker = jdbc.getDataSource().getConnection(); var statement = blocker.createStatement()) {
      statement.execute("BEGIN IMMEDIATE");
      var entered = new java.util.concurrent.CountDownLatch(1);
      var pending = executor.submit(() -> { entered.countDown(); return pacer.reserve(uri,100); });
      assertTrue(entered.await(1,TimeUnit.SECONDS));
      Thread.sleep(300);
      long releasedAt = System.currentTimeMillis();
      statement.execute("COMMIT");
      long slot = pending.get(5,TimeUnit.SECONDS);
      assertTrue(slot >= releasedAt-10, "Reservation timestamp predates lock release by " + (releasedAt-slot));
      assertTrue(pacer.reserve(uri,100)-slot >= 100);
    } finally { executor.shutdownNow(); }
  }

  @Test
  void productionFloorCannotBeConfiguredBelowFiveSeconds() {
    RequestPacer pacer = new RequestPacer(0, new MockEnvironment(), new org.springframework.jdbc.core.JdbcTemplate());

    assertEquals(5_000, pacer.floorMillis());
  }

  @Test
  void testProfileMayDisableTheFloor() {
    MockEnvironment environment = new MockEnvironment();
    environment.setActiveProfiles("test");

    RequestPacer pacer = new RequestPacer(0, environment, new org.springframework.jdbc.core.JdbcTemplate());

    assertEquals(0, pacer.floorMillis());
  }
}
