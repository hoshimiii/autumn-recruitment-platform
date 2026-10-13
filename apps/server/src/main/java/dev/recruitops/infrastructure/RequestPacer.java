package dev.recruitops.infrastructure;

import dev.recruitops.application.CollectionException;
import dev.recruitops.application.SourcePolicy;
import dev.recruitops.domain.Source;
import java.net.URI;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Component;
import org.springframework.jdbc.core.JdbcTemplate;

@Component
public class RequestPacer {
  private final long floorMillis;
  private final JdbcTemplate jdbc;

  public RequestPacer(
      @Value("${recruitops.collector.minimum-floor-seconds:5}") long floorSeconds,
      Environment environment, JdbcTemplate jdbc) {
    long requiredFloor = environment.acceptsProfiles(Profiles.of("test")) ? 0 : 5;
    this.floorMillis = Math.max(requiredFloor, floorSeconds) * 1000;
    this.jdbc = jdbc;
  }

  long floorMillis() { return floorMillis; }

  public long reserve(URI uri, long intervalMillis) {
    long interval = Math.max(floorMillis, Math.max(0, intervalMillis));
    // One SQL statement serializes all processes, including HTTP robots reads.
    // Honor both adjacent reads' intervals when sources have different settings.
    // Sample time inside SQLite after acquiring the write lock, not before waiting on it.
    return jdbc.queryForObject("""
        INSERT INTO origin_request_pacing(origin,last_read_at,interval_millis)
        VALUES (?,CAST(unixepoch('subsec')*1000 AS INTEGER),?)
        ON CONFLICT(origin) DO UPDATE SET
        last_read_at=MAX(excluded.last_read_at,origin_request_pacing.last_read_at+
          MAX(origin_request_pacing.interval_millis,excluded.interval_millis)),
        interval_millis=excluded.interval_millis RETURNING last_read_at
        """, Long.class, SourcePolicy.origin(uri), interval);
  }

  public void await(Source source, URI uri, long robotsDelayMillis) {
    long scheduled = reserve(uri, Math.max(source.minIntervalSeconds() * 1000L, robotsDelayMillis));
    long wait = scheduled - System.currentTimeMillis();
    if (wait > 0) {
      try {
        Thread.sleep(wait);
      } catch (InterruptedException error) {
        Thread.currentThread().interrupt();
        throw CollectionException.failed("request_pacing_interrupted", error);
      }
    }
  }
}
