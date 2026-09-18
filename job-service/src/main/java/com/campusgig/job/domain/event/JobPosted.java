package com.campusgig.job.domain.event;

import com.campusgig.job.domain.Job;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Domain event published to Kafka topic "job-posted".
 */
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

    public static JobPosted from(Job job) {
        return new JobPosted(
                job.getId(),
                job.getEmployerId(),
                job.getTitle(),
                job.getDescription(),
                job.getCategory(),
                job.getLocation(),
                job.getPay().amount(),
                job.getPay().type().name(),
                job.getPostedAt());
    }
}
