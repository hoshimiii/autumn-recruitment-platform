package dev.recruitops.domain;

import com.fasterxml.jackson.databind.JsonNode;

public record ReviewView(long id, Long sourceId, String sourceName, String companyName, String title,
                         String reason, Double confidence, ReviewStatus status, String createdAt,
                         JsonNode normalizedPayload) {}
