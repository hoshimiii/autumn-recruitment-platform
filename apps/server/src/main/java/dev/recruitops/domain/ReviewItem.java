package dev.recruitops.domain;

import com.fasterxml.jackson.databind.JsonNode;

public record ReviewItem(long id, long sourceId, ReviewStatus status, JsonNode candidatePayload,
                         JsonNode normalizedPayload, String reviewNotes, Long publishedJobId,
                         String dedupeKey, String contentHash) {}
