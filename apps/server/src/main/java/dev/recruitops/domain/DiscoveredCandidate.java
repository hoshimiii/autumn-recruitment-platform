package dev.recruitops.domain;

import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.Map;

public record DiscoveredCandidate(
    Map<String, Object> candidatePayload, ObjectNode normalizedPayload, String contentHash,
    String dedupeKey, double confidence) {}
