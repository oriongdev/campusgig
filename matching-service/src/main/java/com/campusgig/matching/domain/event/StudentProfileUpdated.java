package com.campusgig.matching.domain.event;

import java.time.Instant;
import java.util.Set;

/** Consumed from Kafka topic "student-profile-updated" (published by user-service). */
public record StudentProfileUpdated(
        Long studentId,
        String name,
        Set<String> skills,
        String availability,
        Instant occurredAt) {
}
