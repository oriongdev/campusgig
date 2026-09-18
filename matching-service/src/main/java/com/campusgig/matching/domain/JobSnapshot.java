package com.campusgig.matching.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

/**
 * Local read model of an open job, built from JobPosted events.
 * job-service still owns the real Job; this is only a copy for matching.
 */
@Entity
public class JobSnapshot {

    @Id
    private Long jobId;

    private String title;

    @Column(length = 2000)
    private String description;

    private String category;

    private String location;

    protected JobSnapshot() {
        // for JPA
    }

    public JobSnapshot(Long jobId, String title, String description, String category, String location) {
        this.jobId = jobId;
        this.title = title;
        this.description = description;
        this.category = category;
        this.location = location;
    }

    public Long getJobId() {
        return jobId;
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
}
