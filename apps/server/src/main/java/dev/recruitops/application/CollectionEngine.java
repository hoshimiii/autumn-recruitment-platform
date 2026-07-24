package dev.recruitops.application;

import dev.recruitops.domain.DiscoveredCandidate;
import dev.recruitops.domain.Source;
import dev.recruitops.infrastructure.AdapterRegistry;
import dev.recruitops.infrastructure.FetchResult;
import dev.recruitops.infrastructure.RobotsPolicy;
import dev.recruitops.infrastructure.SafeHttpFetcher;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class CollectionEngine {
  private final SafeHttpFetcher fetcher;
  private final RobotsPolicy robots;
  private final AdapterRegistry adapters;
  private final CandidateNormalizer normalizer;

  public CollectionEngine(SafeHttpFetcher fetcher, RobotsPolicy robots, AdapterRegistry adapters,
                          CandidateNormalizer normalizer) {
    this.fetcher = fetcher;
    this.robots = robots;
    this.adapters = adapters;
    this.normalizer = normalizer;
  }

  public Discovery discover(Source source, boolean conditional) {
    RobotsPolicy.Access access = robots.check(source);
    List<DiscoveredCandidate> candidates = new ArrayList<>();
    String etag = null;
    String lastModified = null;
    boolean paged = source.listUrl().contains("{page}");
    for (int page = 1; page <= source.maxPages() && candidates.size() < source.maxItems(); page++) {
      if (page > 1 && !paged) break;
      URI pageUri = SourcePolicy.valid("list_url", source.listUrl().replace("{page}", Integer.toString(page)));
      access.requireAllowed(pageUri);
      FetchResult response = fetcher.fetch(source, pageUri, conditional && page == 1,
          access.crawlDelayMillis(), true);
      if (page == 1) {
        etag = response.etag();
        lastModified = response.lastModified();
      }
      if (response.status() == 304 && page == 1) {
        return new Discovery(List.of(), source.etag(), source.lastModified(), true);
      }
      if (response.status() < 200 || response.status() >= 300) {
        throw CollectionException.failed("http_" + response.status());
      }
      List<Map<String, Object>> parsed = adapters.adapter(source.mode()).parse(source, response);
      for (Map<String, Object> raw : parsed) {
        if (candidates.size() >= source.maxItems()) break;
        candidates.add(normalizer.normalize(source, raw));
      }
    }
    return new Discovery(candidates, etag, lastModified, false);
  }

  public record Discovery(
      List<DiscoveredCandidate> candidates, String etag, String lastModified, boolean notModified) {}
}
