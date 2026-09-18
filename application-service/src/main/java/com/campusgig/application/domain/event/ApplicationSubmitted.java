package com.campusgig.application.domain.event;

import java.time.Instant;

/**
 * Domain event published to Kafka topic "application-submitted".
 */
public record ApplicationSubmitted(Long applicationId, Long jobId, Long studentId, Instant occurredAt) {
}
