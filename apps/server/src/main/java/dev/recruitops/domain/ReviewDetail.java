package dev.recruitops.domain;

import com.fasterxml.jackson.databind.JsonNode;

public record ReviewDetail(long id, Long sourceId, String sourceName, ReviewStatus status,
                           JsonNode candidatePayload, JsonNode normalizedPayload, String reason,
                           Double confidence, String sourceUrl, String dedupeKey,
                           String contentHash, Long duplicateJobId, String createdAt) {}
