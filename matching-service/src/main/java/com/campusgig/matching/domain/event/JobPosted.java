package com.campusgig.matching.domain.event;

import java.math.BigDecimal;
import java.time.Instant;

/** Consumed from Kafka topic "job-posted" (published by job-service). */
public record JobPosted(
        Long jobId,
        Long employerId,
        String title,
        String description,
        String category,
        String location,
        BigDecimal payAmount,
        String payType,
        Instant occurredAt) {
}
