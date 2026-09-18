package com.campusgig.application.presentation.dto;

import com.campusgig.application.domain.ApplicationStatus;
import com.campusgig.application.domain.JobApplication;

import java.time.Instant;

public record ApplicationResponse(
        Long id,
        Long jobId,
        Long studentId,
        String coverMessage,
        ApplicationStatus status,
        Instant submittedAt,
        Instant updatedAt) {

    public static ApplicationResponse from(JobApplication application) {
        return new ApplicationResponse(
                application.getId(),
                application.getJobId(),
                application.getStudentId(),
                application.getCoverMessage(),
                application.getStatus(),
                application.getSubmittedAt(),
                application.getUpdatedAt());
    }
}
