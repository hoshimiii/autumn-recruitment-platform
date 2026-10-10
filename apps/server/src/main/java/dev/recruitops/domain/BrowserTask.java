package dev.recruitops.domain;

public record BrowserTask(long id, long sourceId, int sourceVersion, String termsVersion, String origin,
    String listUrl, int rowStart, int rowEnd, int maxRows, int maxChars, String adapter, String status,
    String workerId, String leaseToken, Long leaseExpiresAt, String selectors, int minIntervalSeconds,
    String robotsUrl, boolean fixtureMode) {}
