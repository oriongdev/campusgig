package com.campusgig.job.domain.event;

import java.time.Instant;

/**
 * Domain event published to Kafka topic "job-closed".
 */
public record JobClosed(Long jobId, Instant occurredAt) {
}
