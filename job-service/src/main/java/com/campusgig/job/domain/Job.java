package com.campusgig.job.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Aggregate root for a job listing (casual job, internship or short gig).
 */
@Entity
public class Job {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long employerId;

    private String title;

    @Column(length = 2000)
    private String description;

    private String category;

    private String location;

    @Embedded
    private Pay pay;

    @Enumerated(EnumType.STRING)
    private JobStatus status;

    private Instant postedAt;

    private Instant closedAt;

    protected Job() {
        // for JPA
    }

    public Job(Long employerId, String title, String description, String category, String location, Pay pay) {
        if (employerId == null) {
            throw new IllegalArgumentException("Employer id is required");
        }
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("Job title is required");
        }
        if (category == null || category.isBlank()) {
            throw new IllegalArgumentException("Job category is required");
        }
        if (location == null || location.isBlank()) {
            throw new IllegalArgumentException("Job location is required");
        }
        if (pay == null) {
            throw new IllegalArgumentException("Pay is required");
        }
        this.employerId = employerId;
        this.title = title.trim();
        this.description = description == null ? "" : description.trim();
        this.category = category.trim().toLowerCase();
        this.location = location.trim();
        this.pay = pay;
        this.status = JobStatus.OPEN;
        this.postedAt = Instant.now();
    }

    /** Updates the listing's details. Only an open job can be edited. */
    public void update(String title, String description, String category, String location, Pay pay) {
        if (status == JobStatus.CLOSED) {
            throw new IllegalStateException("Job " + id + " is closed and cannot be edited");
        }
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("Job title is required");
        }
        if (category == null || category.isBlank()) {
            throw new IllegalArgumentException("Job category is required");
        }
        if (location == null || location.isBlank()) {
            throw new IllegalArgumentException("Job location is required");
        }
        if (pay == null) {
            throw new IllegalArgumentException("Pay is required");
        }
        this.title = title.trim();
        this.description = description == null ? "" : description.trim();
        this.category = category.trim().toLowerCase();
        this.location = location.trim();
        this.pay = pay;
    }

    /** Closes the listing so no new applications are accepted. */
    public void close() {
        if (status == JobStatus.CLOSED) {
            throw new IllegalStateException("Job " + id + " is already closed");
        }
        this.status = JobStatus.CLOSED;
        this.closedAt = Instant.now();
    }

    public boolean isOpen() {
        return status == JobStatus.OPEN;
    }

    /** Search rule: blank filters match everything. */
    public boolean matches(String categoryFilter, String locationFilter, BigDecimal minPay) {
        boolean categoryOk = categoryFilter == null || categoryFilter.isBlank()
                || category.equalsIgnoreCase(categoryFilter.trim());
        boolean locationOk = locationFilter == null || locationFilter.isBlank()
                || location.toLowerCase().contains(locationFilter.trim().toLowerCase());
        boolean payOk = minPay == null || pay.amount().compareTo(minPay) >= 0;
        return categoryOk && locationOk && payOk;
    }

    public Long getId() {
        return id;
    }

    public Long getEmployerId() {
        return employerId;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getCategory() {
        return category;
    }

    public String getLocation() {
        return location;
    }

    public Pay getPay() {
        return pay;
    }

    public JobStatus getStatus() {
        return status;
    }

    public Instant getPostedAt() {
        return postedAt;
    }

    public Instant getClosedAt() {
        return closedAt;
    }
}
