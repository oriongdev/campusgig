package com.campusgig.matching.domain.event;

import java.time.Instant;

/** Consumed from Kafka topic "application-submitted" (published by application-service). */
public record ApplicationSubmitted(Long applicationId, Long jobId, Long studentId, Instant occurredAt) {
}
