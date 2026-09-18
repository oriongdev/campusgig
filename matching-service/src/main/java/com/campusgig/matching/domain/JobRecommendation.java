package com.campusgig.matching.domain;

/**
 * Value object: one recommended job for a student, with a score from 0 to 1 and the reason.
 */
public record JobRecommendation(Long jobId, String title, String category, double score, String reason) {
}
