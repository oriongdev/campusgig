package com.campusgig.job.infrastructure.messaging;

import com.campusgig.job.domain.event.JobClosed;
import com.campusgig.job.domain.event.JobPosted;
import com.campusgig.job.domain.event.JobUpdated;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.stream.function.StreamBridge;

import java.math.BigDecimal;
import java.time.Instant;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/** Each domain event must go out on its own binding, which application.yml maps to a Kafka topic. */
class JobEventPublisherTest {

    private final StreamBridge streamBridge = mock(StreamBridge.class);
    private final JobEventPublisher publisher = new JobEventPublisher(streamBridge);

    @Test
    void jobPostedGoesToTheJobPostedBinding() {
        JobPosted event = new JobPosted(1L, 2L, "Barista", "", "hospitality", "Wollongong",
                new BigDecimal("28.50"), "HOURLY", Instant.now());

        publisher.publish(event);

        verify(streamBridge).send("jobPosted-out-0", event);
    }

    @Test
    void jobUpdatedGoesToTheJobUpdatedBinding() {
        JobUpdated event = new JobUpdated(1L, "Barista", "", "hospitality", "Wollongong",
                new BigDecimal("30.00"), "HOURLY", Instant.now());

        publisher.publish(event);

        verify(streamBridge).send("jobUpdated-out-0", event);
    }

    @Test
    void jobClosedGoesToTheJobClosedBinding() {
        JobClosed event = new JobClosed(1L, Instant.now());

        publisher.publish(event);

        verify(streamBridge).send("jobClosed-out-0", event);
    }
}
