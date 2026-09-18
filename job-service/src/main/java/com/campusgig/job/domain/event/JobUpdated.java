package com.campusgig.job.domain.event;

import com.campusgig.job.domain.Job;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Domain event published to Kafka topic "job-updated" when an employer edits a listing.
 */
public record JobUpdated(
        Long jobId,
        String title,
        String description,
        String category,
        String location,
        BigDecimal payAmount,
        String payType,
        Instant occurredAt) {

    public static JobUpdated from(Job job) {
        return new JobUpdated(
                job.getId(),
                job.getTitle(),
                job.getDescription(),
                job.getCategory(),
                job.getLocation(),
                job.getPay().amount(),
                job.getPay().type().name(),
                Instant.now());
    }
}
