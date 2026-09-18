package com.campusgig.matching.domain;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;

import java.util.HashSet;
import java.util.Set;

/**
 * Local read model of a student profile, built from StudentProfileUpdated events.
 * user-service still owns the real Student.
 */
@Entity
public class StudentSnapshot {

    @Id
    private Long studentId;

    private String name;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "student_snapshot_skills", joinColumns = @JoinColumn(name = "student_id"))
    @Column(name = "skill")
    private Set<String> skills = new HashSet<>();

    private String availability;

    protected StudentSnapshot() {
        // for JPA
    }

    public StudentSnapshot(Long studentId) {
        this.studentId = studentId;
    }

    public void update(String name, Set<String> newSkills, String availability) {
        this.name = name;
        this.skills.clear();
        if (newSkills != null) {
            this.skills.addAll(newSkills);
        }
        this.availability = availability;
    }

    public Long getStudentId() {
        return studentId;
    }

    public String getName() {
        return name;
    }

    public Set<String> getSkills() {
        return Set.copyOf(skills);
    }

    public String getAvailability() {
        return availability;
    }
}
