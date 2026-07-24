package dev.recruitops.domain;

public record Source(
    long id, String name, String type, String baseUrl, String listUrl, String adapterType, String mode,
    String complianceStatus, String trustLevel, String companyCategory, boolean enabled, String cron,
    String timezone, int minIntervalSeconds, int maxPages, int maxItems, String selectors,
    String fieldMapping, String termsUrl, String termsConfirmedAt, String robotsUrl, int timeoutSeconds,
    String etag, String lastModified, String lastRunAt, String lastStatus, boolean aiEnabled,
    String aiProvider, String aiModel, int configVersion) {}
