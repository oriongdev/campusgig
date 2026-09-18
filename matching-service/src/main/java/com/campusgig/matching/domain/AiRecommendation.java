package com.campusgig.matching.domain;

/** A job recommendation produced by the AI matching agent. */
public record AiRecommendation(Long jobId, String title, Double score, String reason) {
}
