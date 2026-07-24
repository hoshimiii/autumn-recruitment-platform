package dev.recruitops.infrastructure;

import crawlercommons.robots.BaseRobotRules;
import crawlercommons.robots.SimpleRobotRulesParser;
import dev.recruitops.application.CollectionException;
import dev.recruitops.application.SourcePolicy;
import dev.recruitops.domain.Source;
import java.net.URI;
import java.util.Locale;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class RobotsPolicy {
  private final SafeHttpFetcher fetcher;
  private final String productToken;

  public RobotsPolicy(
      SafeHttpFetcher fetcher,
      @Value("${recruitops.collector.user-agent:AutumnRecruitmentBot/1.0}") String userAgent) {
    this.fetcher = fetcher;
    String token = userAgent.split("/", 2)[0].replaceAll("[^A-Za-z_-]", "");
    this.productToken = (token.isBlank() ? "AutumnRecruitmentBot" : token).toLowerCase(Locale.ROOT);
  }

  public Access check(Source source) {
    URI robotsUri = robotsUri(source);
    FetchResult response;
    try {
      response = fetcher.fetch(source, robotsUri, false, 0, false);
    } catch (CollectionException error) {
      if ("BLOCKED".equals(error.status())) throw error;
      throw CollectionException.blocked("robots_unavailable");
    }
    if (response.status() == 404) return new Access(null, 0);
    if (response.status() != 200) {
      throw CollectionException.blocked("robots_http_" + response.status());
    }
    try {
      SimpleRobotRulesParser parser = new SimpleRobotRulesParser();
      BaseRobotRules rules = parser.parseContent(robotsUri.toString(), response.body(),
          response.contentType(), Set.of(productToken));
      return new Access(rules, Math.max(0, rules.getCrawlDelay()));
    } catch (RuntimeException error) {
      throw CollectionException.blocked("robots_parse_failed");
    }
  }

  private static URI robotsUri(Source source) {
    if (source.robotsUrl() != null && !source.robotsUrl().isBlank()) {
      return SourcePolicy.valid("robots_url", source.robotsUrl());
    }
    URI base = SourcePolicy.valid("base_url", source.baseUrl());
    return URI.create(SourcePolicy.origin(base) + "/robots.txt");
  }

  public record Access(BaseRobotRules rules, long crawlDelayMillis) {
    public void requireAllowed(URI uri) {
      if (rules != null && !rules.isAllowed(uri.toString())) {
        throw CollectionException.blocked("robots_denied");
      }
    }
  }
}
