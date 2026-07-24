package dev.recruitops.domain;

import java.util.List;

public record ManualItem(long id, String platform, String companyName, String jobTitle,
    List<String> searchKeywords, String sourceUrl, List<String> techStackKeywords,
    String certificateNotes, String requiredAction, boolean requiresLogin, boolean requiresCaptcha,
    String applyMethod, String reminderAt, int priority, String status, String notes, String createdAt) {}
