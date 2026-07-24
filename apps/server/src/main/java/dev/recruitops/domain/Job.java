package dev.recruitops.domain;

import java.util.List;

public record Job(long id, String companyCategory, String companyName, String title, String jobFamily,
                  String jobType, String city, String deadline, String status, int priority,
                  List<String> techStack, List<String> certificateRequirements, String sourceUrl, String applyUrl,
                  String applyMethod, boolean requiresLogin, boolean requiresCaptcha, String sourceTrust,
                  String sourceName, String sourceType, String capturedAt, String updatedAt,
                  String reviewStatus, Double confidence, String jd, String requirements) {}
