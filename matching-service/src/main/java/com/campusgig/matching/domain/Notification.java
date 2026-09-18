package com.campusgig.matching.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

import java.time.Instant;

/**
 * A message shown to a student, e.g. when their application status changes.
 */
@Entity
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long studentId;

    @Column(length = 500)
    private String message;

    private Instant createdAt;

    protected Notification() {
        // for JPA
    }

    public Notification(Long studentId, String message) {
        if (studentId == null) {
            throw new IllegalArgumentException("Student id is required");
        }
        this.studentId = studentId;
        this.message = message;
        this.createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public Long getStudentId() {
        return studentId;
    }

    public String getMessage() {
        return message;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
