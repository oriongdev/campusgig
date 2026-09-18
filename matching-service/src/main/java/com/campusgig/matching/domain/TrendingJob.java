package com.campusgig.matching.domain;

/** A job attracting unusual interest in the recent time window (requirement 12). */
public record TrendingJob(Long jobId, String title, long applicationsInWindow) {
}
