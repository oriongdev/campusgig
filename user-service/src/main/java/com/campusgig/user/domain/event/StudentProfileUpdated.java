package com.campusgig.user.domain.event;

import com.campusgig.user.domain.Student;

import java.time.Instant;
import java.util.Set;

/**
 * Domain event published to Kafka topic "student-profile-updated".
 */
public record StudentProfileUpdated(
        Long studentId,
        String name,
        Set<String> skills,
        String availability,
        Instant occurredAt) {

    public static StudentProfileUpdated from(Student student) {
        return new StudentProfileUpdated(
                student.getId(),
                student.getName(),
                student.getSkills(),
                student.getAvailability().name(),
                Instant.now());
    }
}
