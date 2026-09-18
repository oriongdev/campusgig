package com.campusgig.matching.domain;

import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SkillMatcherTest {

    private final SkillMatcher matcher = new SkillMatcher();

    private StudentSnapshot student(Set<String> skills) {
        StudentSnapshot snapshot = new StudentSnapshot(1L);
        snapshot.update("Alex", skills, "WEEKENDS");
        return snapshot;
    }

    private JobSnapshot baristaJob() {
        return new JobSnapshot(7L, "Weekend Barista", "Coffee and customer service", "hospitality", "Wollongong");
    }

    @Test
    void scoresMatchingSkills() {
        Optional<JobRecommendation> result = matcher.match(student(Set.of("barista", "java")), baristaJob());
        assertTrue(result.isPresent());
        assertEquals(7L, result.get().jobId().longValue());
        assertEquals(0.5, result.get().score());
        assertTrue(result.get().reason().contains("barista"));
    }

    @Test
    void perfectMatchScoresOne() {
        Optional<JobRecommendation> result = matcher.match(student(Set.of("barista")), baristaJob());
        assertEquals(1.0, result.orElseThrow().score());
    }

    @Test
    void noOverlapMeansNoRecommendation() {
        assertTrue(matcher.match(student(Set.of("welding")), baristaJob()).isEmpty());
    }

    @Test
    void studentWithoutSkillsGetsNothing() {
        assertTrue(matcher.match(student(Set.of()), baristaJob()).isEmpty());
    }
}
