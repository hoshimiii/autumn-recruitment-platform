package dev.recruitops.infrastructure;

import dev.recruitops.application.CollectionException;
import dev.recruitops.application.SourcePolicy;
import dev.recruitops.domain.Source;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Component;

@Component
public class RequestPacer {
  private final long floorMillis;
  private final Map<String, Long> lastRequest = new HashMap<>();

  public RequestPacer(
      @Value("${recruitops.collector.minimum-floor-seconds:5}") long floorSeconds,
      Environment environment) {
    long requiredFloor = environment.acceptsProfiles(Profiles.of("test")) ? 0 : 5;
    this.floorMillis = Math.max(requiredFloor, floorSeconds) * 1000;
  }

  long floorMillis() { return floorMillis; }

  public synchronized void await(Source source, URI uri, long robotsDelayMillis) {
    long interval = Math.max(floorMillis,
        Math.max(source.minIntervalSeconds() * 1000L, Math.max(0, robotsDelayMillis)));
    String origin = SourcePolicy.origin(uri);
    long wait = lastRequest.getOrDefault(origin, 0L) + interval - System.currentTimeMillis();
    if (wait > 0) {
      try {
        Thread.sleep(wait);
      } catch (InterruptedException error) {
        Thread.currentThread().interrupt();
        throw CollectionException.failed("request_pacing_interrupted", error);
      }
    }
    lastRequest.put(origin, System.currentTimeMillis());
  }
}
