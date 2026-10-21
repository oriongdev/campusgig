package com.campusgig.matching.presentation;

import com.campusgig.matching.domain.TrendingJob;
import com.campusgig.matching.service.AnalyticsQueryService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * Real-time analysis endpoints (requirements 11 and 12).
 * {@link AnalyticsQueryService} decides whether Kafka Streams or the in-memory counters answer.
 */
@RestController
@RequestMapping("/api/analytics")
public class AnalyticsController {

    private final AnalyticsQueryService analytics;

    public AnalyticsController(AnalyticsQueryService analytics) {
        this.analytics = analytics;
    }

    /** Says which implementation is answering right now, so a demo can prove it. */
    @GetMapping("/source")
    public Map<String, String> source() {
        return Map.of("source", analytics.source());
    }

    /** Live count of jobs posted per category (requirement 11). */
    @GetMapping("/jobs-per-category")
    public Map<String, Long> jobsPerCategory() {
        return analytics.jobsPerCategory();
    }

    /** Live count of applications per job (requirement 12). */
    @GetMapping("/applications-per-job")
    public Map<Long, Long> applicationsPerJob() {
        return analytics.applicationsPerJob();
    }

    /** Jobs attracting the most applications in the last 15 minutes (requirement 12). */
    @GetMapping("/trending-jobs")
    public List<TrendingJob> trendingJobs() {
        return analytics.trendingJobs();
    }
}
