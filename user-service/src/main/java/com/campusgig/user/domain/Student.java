package com.campusgig.user.domain;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;

import java.util.HashSet;
import java.util.Set;

/**
 * Aggregate root for a student's profile.
 */
@Entity
public class Student {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @Embedded
    private EmailAddress email;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "student_skills", joinColumns = @JoinColumn(name = "student_id"))
    @Column(name = "skill")
    private Set<String> skills = new HashSet<>();

    @Enumerated(EnumType.STRING)
    private Availability availability;

    protected Student() {
        // for JPA
    }

    public Student(String name, EmailAddress email, Set<String> skills, Availability availability) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Student name is required");
        }
        if (email == null) {
            throw new IllegalArgumentException("Student email is required");
        }
        this.name = name.trim();
        this.email = email;
        applyProfile(skills, availability);
    }

    /** Replaces the student's skills and availability. Skills are stored lower-case. */
    public void updateProfile(Set<String> newSkills, Availability newAvailability) {
        applyProfile(newSkills, newAvailability);
    }

    private void applyProfile(Set<String> newSkills, Availability newAvailability) {
        this.skills.clear();
        if (newSkills != null) {
            newSkills.stream()
                    .filter(skill -> skill != null && !skill.isBlank())
                    .map(skill -> skill.trim().toLowerCase())
                    .forEach(this.skills::add);
        }
        this.availability = newAvailability == null ? Availability.FLEXIBLE : newAvailability;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public EmailAddress getEmail() {
        return email;
    }

    public Set<String> getSkills() {
        return Set.copyOf(skills);
    }

    public Availability getAvailability() {
        return availability;
    }
}
