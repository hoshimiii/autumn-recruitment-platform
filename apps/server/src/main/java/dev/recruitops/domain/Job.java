package dev.recruitops.domain;

import java.util.List;

public record Job(long id, String companyType, String companyName, String title, String jobType, String city,
                  String deadline, String status, List<String> techStack, List<String> certificateRequirements,
                  String sourceUrl, String applyUrl, String sourceTrustLevel, String jd, String requirements) {}
