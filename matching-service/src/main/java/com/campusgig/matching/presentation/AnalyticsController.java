package com.campusgig.matching.presentation;

import com.campusgig.matching.domain.TrendingJob;
import com.campusgig.matching.infrastructure.streams.StreamAnalyticsService;
import com.campusgig.matching.service.AnalyticsService;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * Real-time analysis endpoints.
 *
 * <p>Under the "streams" profile the numbers come from Kafka Streams state stores.
 * Otherwise, and while those stores are still warming up, they come from the in-memory counters.
 */
@RestController
@RequestMapping("/api/analytics")
public class AnalyticsController {

    private final AnalyticsService analyticsService;
    private final ObjectProvider<StreamAnalyticsService> streamAnalytics;

    public AnalyticsController(AnalyticsService analyticsService,
                               ObjectProvider<StreamAnalyticsService> streamAnalytics) {
        this.analyticsService = analyticsService;
        this.streamAnalytics = streamAnalytics;
    }

    @GetMapping("/source")
    public Map<String, String> source() {
        return Map.of("source", streamAnalytics.getIfAvailable() == null ? "in-memory counters" : "kafka streams");
    }

    @GetMapping("/jobs-per-category")
    public Map<String, Long> jobsPerCategory() {
        StreamAnalyticsService streams = streamAnalytics.getIfAvailable();
        if (streams != null) {
            Map<String, Long> fromStreams = streams.jobsPerCategory();
            if (!fromStreams.isEmpty()) {
                return fromStreams;
            }
        }
        return analyticsService.jobsPerCategory();
    }

    @GetMapping("/applications-per-job")
    public Map<Long, Long> applicationsPerJob() {
        StreamAnalyticsService streams = streamAnalytics.getIfAvailable();
        if (streams != null) {
            Map<Long, Long> fromStreams = streams.applicationsPerJob();
            if (!fromStreams.isEmpty()) {
                return fromStreams;
            }
        }
        return analyticsService.applicationsPerJob();
    }

    /** Jobs attracting the most applications in the last 15 minutes (requirement 12). */
    @GetMapping("/trending-jobs")
    public List<TrendingJob> trendingJobs() {
        StreamAnalyticsService streams = streamAnalytics.getIfAvailable();
        if (streams != null) {
            List<TrendingJob> fromStreams = streams.trendingJobs(AnalyticsService.TRENDING_THRESHOLD);
            if (!fromStreams.isEmpty()) {
                return fromStreams;
            }
        }
        return analyticsService.trendingJobs();
    }
}
