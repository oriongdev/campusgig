package com.campusgig.matching.presentation.dto;

import com.campusgig.matching.domain.Notification;

import java.time.Instant;

public record NotificationResponse(Long id, Long studentId, String message, Instant createdAt) {

    public static NotificationResponse from(Notification notification) {
        return new NotificationResponse(
                notification.getId(),
                notification.getStudentId(),
                notification.getMessage(),
                notification.getCreatedAt());
    }
}
