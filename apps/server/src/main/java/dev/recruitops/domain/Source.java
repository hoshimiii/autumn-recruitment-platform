package dev.recruitops.domain;

public record Source(long id, String name, String type, String baseUrl, String adapterType, String mode,
                     String complianceStatus, String trustLevel, String scope, boolean enabled, int configVersion) {}
