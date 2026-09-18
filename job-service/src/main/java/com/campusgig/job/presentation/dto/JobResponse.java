package com.campusgig.job.presentation.dto;

import com.campusgig.job.domain.Job;
import com.campusgig.job.domain.JobStatus;
import com.campusgig.job.domain.PayType;

import java.math.BigDecimal;
import java.time.Instant;

public record JobResponse(
        Long id,
        Long employerId,
        String title,
        String description,
        String category,
        String location,
        BigDecimal payAmount,
        PayType payType,
        JobStatus status,
        Instant postedAt) {

    public static JobResponse from(Job job) {
        return new JobResponse(
                job.getId(),
                job.getEmployerId(),
                job.getTitle(),
                job.getDescription(),
                job.getCategory(),
                job.getLocation(),
                job.getPay().amount(),
                job.getPay().type(),
                job.getStatus(),
                job.getPostedAt());
    }
}
