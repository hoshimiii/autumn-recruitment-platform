package dev.recruitops.domain;

public record AgentSettings(
    long id, boolean enabled, String baseUrl, String secretEnvRef, String scanModel,
    String scanReasoningEffort, String processModel, String processReasoningEffort,
    int timeoutSeconds, int configVersion, String updatedAt) {}
