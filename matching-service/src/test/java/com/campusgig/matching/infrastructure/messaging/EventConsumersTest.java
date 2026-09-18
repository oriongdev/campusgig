package com.campusgig.matching.infrastructure.messaging;

import com.campusgig.matching.domain.event.ApplicationStatusChanged;
import com.campusgig.matching.domain.event.ApplicationSubmitted;
import com.campusgig.matching.domain.event.JobPosted;
import com.campusgig.matching.service.AnalyticsService;
import com.campusgig.matching.service.MatchingService;
import com.campusgig.matching.service.NotificationService;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.function.Consumer;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class EventConsumersTest {

    private final EventConsumers consumers = new EventConsumers();
    private final MatchingService matching = mock(MatchingService.class);
    private final AnalyticsService analytics = mock(AnalyticsService.class);
    private final NotificationService notifications = mock(NotificationService.class);

    @Test
    void jobPostedUpdatesTheSnapshotAndTheCounts() {
        JobPosted event = new JobPosted(1L, 2L, "Weekend Barista", "", "hospitality", "Wollongong",
                new BigDecimal("28.50"), "HOURLY", Instant.now());

        Consumer<JobPosted> consumer = consumers.onJobPosted(matching, analytics);
        consumer.accept(event);

        verify(matching).recordJob(event);
        verify(analytics).recordJobPosted(event);
    }

    @Test
    void applicationSubmittedUpdatesTheCounts() {
        ApplicationSubmitted event = new ApplicationSubmitted(1L, 2L, 3L, Instant.now());

        consumers.onApplicationSubmitted(analytics).accept(event);

        verify(analytics).recordApplicationSubmitted(event);
    }

    @Test
    void statusChangeCreatesANotification() {
        ApplicationStatusChanged event = new ApplicationStatusChanged(
                1L, 2L, 3L, "SUBMITTED", "ACCEPTED", Instant.now());

        consumers.onApplicationStatusChanged(notifications).accept(event);

        verify(notifications).notifyStatusChange(event);
    }
}
