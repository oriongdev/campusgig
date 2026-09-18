package com.campusgig.application.domain.event;

import java.time.Instant;

/**
 * Domain event published to Kafka topic "application-status-changed".
 */
public record ApplicationStatusChanged(
        Long applicationId,
        Long jobId,
        Long studentId,
        String oldStatus,
        String newStatus,
        Instant occurredAt) {
}
