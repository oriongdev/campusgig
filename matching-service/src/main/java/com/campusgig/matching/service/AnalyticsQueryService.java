package com.campusgig.matching.service;

import com.campusgig.matching.domain.TrendingJob;
import com.campusgig.matching.infrastructure.streams.StreamAnalyticsService;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Answers the real-time analysis queries (requirements 11 and 12) from whichever implementation
 * is live.
 *
 * <p>Under the "streams" profile the numbers come from Kafka Streams state stores.
 * Otherwise, and for the few seconds those stores take to become queryable after start-up,
 * they come from the in-memory counters kept by {@link AnalyticsService}.
 */
@Service
public class AnalyticsQueryService {

    public static final String SOURCE_COUNTERS = "in-memory counters";
    public static final String SOURCE_STREAMS = "kafka streams";
    public static final String SOURCE_STREAMS_STARTING = "in-memory counters (kafka streams is starting)";

    private final AnalyticsService counters;
    private final ObjectProvider<StreamAnalyticsService> streamAnalytics;

    public AnalyticsQueryService(AnalyticsService counters, ObjectProvider<StreamAnalyticsService> streamAnalytics) {
        this.counters = counters;
        this.streamAnalytics = streamAnalytics;
    }

    /** Says which implementation is answering right now, so a demo can prove it. */
    public String source() {
        StreamAnalyticsService streams = streamAnalytics.getIfAvailable();
        if (streams == null) {
            return SOURCE_COUNTERS;
        }
        return streams.isReady() ? SOURCE_STREAMS : SOURCE_STREAMS_STARTING;
    }

    /** Live count of jobs posted per category (requirement 11). */
    public Map<String, Long> jobsPerCategory() {
        return fromStreams().flatMap(StreamAnalyticsService::jobsPerCategory)
                .orElseGet(counters::jobsPerCategory);
    }

    /** Live count of applications per job (requirement 12). */
    public Map<Long, Long> applicationsPerJob() {
        return fromStreams().flatMap(StreamAnalyticsService::applicationsPerJob)
                .orElseGet(counters::applicationsPerJob);
    }

    /** Jobs attracting the most applications in the last 15 minutes (requirement 12). */
    public List<TrendingJob> trendingJobs() {
        return fromStreams().flatMap(streams -> streams.trendingJobs(AnalyticsService.TRENDING_THRESHOLD))
                .orElseGet(counters::trendingJobs);
    }

    private Optional<StreamAnalyticsService> fromStreams() {
        return Optional.ofNullable(streamAnalytics.getIfAvailable());
    }
}
