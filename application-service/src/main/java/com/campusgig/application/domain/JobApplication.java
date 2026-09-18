package com.campusgig.application.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

import java.time.Instant;

/**
 * Aggregate root for a student's application to a job.
 * Jobs and students are referenced by id only, because they belong to other bounded contexts.
 */
@Entity
public class JobApplication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long jobId;

    private Long studentId;

    @Column(length = 2000)
    private String coverMessage;

    @Enumerated(EnumType.STRING)
    private ApplicationStatus status;

    private Instant submittedAt;

    private Instant updatedAt;

    protected JobApplication() {
        // for JPA
    }

    public JobApplication(Long jobId, Long studentId, String coverMessage) {
        if (jobId == null) {
            throw new IllegalArgumentException("Job id is required");
        }
        if (studentId == null) {
            throw new IllegalArgumentException("Student id is required");
        }
        this.jobId = jobId;
        this.studentId = studentId;
        this.coverMessage = coverMessage == null ? "" : coverMessage.trim();
        this.status = ApplicationStatus.SUBMITTED;
        this.submittedAt = Instant.now();
        this.updatedAt = this.submittedAt;
    }

    /**
     * Moves the application to a new status if the transition is allowed.
     *
     * @return the previous status
     */
    public ApplicationStatus moveTo(ApplicationStatus next) {
        if (next == null) {
            throw new IllegalArgumentException("New status is required");
        }
        if (!status.canMoveTo(next)) {
            throw new IllegalStateException(
                    "Cannot change application " + id + " from " + status + " to " + next);
        }
        ApplicationStatus previous = status;
        this.status = next;
        this.updatedAt = Instant.now();
        return previous;
    }

    public Long getId() {
        return id;
    }

    public Long getJobId() {
        return jobId;
    }

    public Long getStudentId() {
        return studentId;
    }

    public String getCoverMessage() {
        return coverMessage;
    }

    public ApplicationStatus getStatus() {
        return status;
    }

    public Instant getSubmittedAt() {
        return submittedAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
