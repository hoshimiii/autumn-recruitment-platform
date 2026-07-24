package dev.recruitops.domain;

public record CollectionRun(long id, Long sourceId, String triggerType, String status, String startedAt,
                            String finishedAt, int discoveredCount, int reviewItemCount, int errorCount,
                            String errorSummary) {}
