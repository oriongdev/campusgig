package com.campusgig.matching.service;

import com.campusgig.matching.domain.TrendingJob;
import com.campusgig.matching.domain.event.ApplicationSubmitted;
import com.campusgig.matching.domain.event.JobPosted;
import com.campusgig.matching.infrastructure.persistence.JobSnapshotRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AnalyticsServiceTest {

    private final JobSnapshotRepository jobs = mock(JobSnapshotRepository.class);
    private final AnalyticsService service = new AnalyticsService(jobs);

    private JobPosted jobPosted(long id, String category) {
        return new JobPosted(id, 1L, "Job " + id, "", category, "Wollongong",
                new BigDecimal("25"), "HOURLY", Instant.now());
    }

    private ApplicationSubmitted applicationFor(long jobId, Instant at) {
        return new ApplicationSubmitted(jobId, jobId, 1L, at);
    }

    @Test
    void countsJobsPerCategory() {
        service.recordJobPosted(jobPosted(1L, "hospitality"));
        service.recordJobPosted(jobPosted(2L, "hospitality"));
        service.recordJobPosted(jobPosted(3L, "tutoring"));

        assertEquals(2L, service.jobsPerCategory().get("hospitality"));
        assertEquals(1L, service.jobsPerCategory().get("tutoring"));
    }

    @Test
    void countsApplicationsPerJob() {
        service.recordApplicationSubmitted(applicationFor(1L, Instant.now()));
        service.recordApplicationSubmitted(applicationFor(1L, Instant.now()));

        assertEquals(2L, service.applicationsPerJob().get(1L));
    }

    @Test
    void jobBecomesTrendingAfterEnoughRecentApplications() {
        when(jobs.findById(anyLong())).thenReturn(Optional.empty());

        service.recordApplicationSubmitted(applicationFor(1L, Instant.now()));
        assertTrue(service.trendingJobs().isEmpty());

        service.recordApplicationSubmitted(applicationFor(1L, Instant.now()));
        List<TrendingJob> trending = service.trendingJobs();
        assertEquals(1, trending.size());
        assertEquals(2L, trending.get(0).applicationsInWindow());
    }

    @Test
    void oldApplicationsDropOutOfTheTrendingWindow() {
        when(jobs.findById(anyLong())).thenReturn(Optional.empty());
        Instant old = Instant.now().minus(AnalyticsService.TRENDING_WINDOW).minusSeconds(60);

        service.recordApplicationSubmitted(applicationFor(1L, old));
        service.recordApplicationSubmitted(applicationFor(1L, old));

        assertTrue(service.trendingJobs().isEmpty());
    }
}
