package dev.recruitops.domain;

import com.fasterxml.jackson.databind.JsonNode;

public record JobSnapshot(long id, Long sourceId, String sourceUrl, String contentHash,
                          JsonNode parsedPayload, String capturedAt) {}
