package com.campusgig.matching.service;

import com.campusgig.matching.domain.TrendingJob;
import com.campusgig.matching.infrastructure.streams.StreamAnalyticsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** Which implementation answers the real-time analysis queries, and when. */
class AnalyticsQueryServiceTest {

    private final AnalyticsService counters = mock(AnalyticsService.class);
    private final StreamAnalyticsService streams = mock(StreamAnalyticsService.class);

    @SuppressWarnings("unchecked")
    private AnalyticsQueryService analyticsWith(StreamAnalyticsService streamService) {
        ObjectProvider<StreamAnalyticsService> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(streamService);
        return new AnalyticsQueryService(counters, provider);
    }

    @Test
    void byDefaultTheInMemoryCountersAnswer() {
        when(counters.jobsPerCategory()).thenReturn(Map.of("hospitality", 2L));
        AnalyticsQueryService analytics = analyticsWith(null);

        assertEquals(Map.of("hospitality", 2L), analytics.jobsPerCategory());
        assertEquals("in-memory counters", analytics.source());
    }

    @Test
    void underTheStreamsProfileTheStateStoresAnswer() {
        when(streams.isReady()).thenReturn(true);
        when(streams.jobsPerCategory()).thenReturn(Optional.of(Map.of("hospitality", 3L)));
        when(streams.applicationsPerJob()).thenReturn(Optional.of(Map.of(1L, 4L)));
        when(streams.trendingJobs(AnalyticsService.TRENDING_THRESHOLD))
                .thenReturn(Optional.of(List.of(new TrendingJob(1L, "Weekend Barista", 4))));
        AnalyticsQueryService analytics = analyticsWith(streams);

        assertEquals(Map.of("hospitality", 3L), analytics.jobsPerCategory());
        assertEquals(Map.of(1L, 4L), analytics.applicationsPerJob());
        assertEquals("Weekend Barista", analytics.trendingJobs().get(0).title());
        assertEquals("kafka streams", analytics.source());
    }

    @Test
    void anEmptyStateStoreIsAValidAnswerAndIsNotReplacedByTheCounters() {
        when(streams.jobsPerCategory()).thenReturn(Optional.of(Map.of()));
        when(counters.jobsPerCategory()).thenReturn(Map.of("stale", 9L));

        assertEquals(Map.of(), analyticsWith(streams).jobsPerCategory());
    }

    @Test
    void whileKafkaStreamsIsStillStartingTheCountersAnswerInstead() {
        when(streams.isReady()).thenReturn(false);
        when(streams.jobsPerCategory()).thenReturn(Optional.empty());
        when(streams.applicationsPerJob()).thenReturn(Optional.empty());
        when(streams.trendingJobs(AnalyticsService.TRENDING_THRESHOLD)).thenReturn(Optional.empty());
        when(counters.jobsPerCategory()).thenReturn(Map.of("retail", 1L));
        when(counters.applicationsPerJob()).thenReturn(Map.of(2L, 1L));
        when(counters.trendingJobs()).thenReturn(List.of());
        AnalyticsQueryService analytics = analyticsWith(streams);

        assertEquals(Map.of("retail", 1L), analytics.jobsPerCategory());
        assertEquals(Map.of(2L, 1L), analytics.applicationsPerJob());
        assertEquals(List.of(), analytics.trendingJobs());
        assertEquals("in-memory counters (kafka streams is starting)", analytics.source());
    }
}
