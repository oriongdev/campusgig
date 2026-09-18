package com.campusgig.matching.domain.event;

import java.time.Instant;

/** Consumed from Kafka topic "job-closed" (published by job-service). */
public record JobClosed(Long jobId, Instant occurredAt) {
}
