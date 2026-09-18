package com.campusgig.matching.domain.event;

import java.time.Instant;

/** Consumed from Kafka topic "application-status-changed" (published by application-service). */
public record ApplicationStatusChanged(
        Long applicationId,
        Long jobId,
        Long studentId,
        String oldStatus,
        String newStatus,
        Instant occurredAt) {
}
