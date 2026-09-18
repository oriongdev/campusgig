package com.campusgig.job.infrastructure.messaging;

import com.campusgig.job.domain.event.JobClosed;
import com.campusgig.job.domain.event.JobPosted;
import com.campusgig.job.domain.event.JobUpdated;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.stereotype.Component;

/**
 * Publishes job domain events to Kafka through Spring Cloud Stream.
 * Binding-to-topic mapping lives in application.yml.
 */
@Component
public class JobEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(JobEventPublisher.class);

    private final StreamBridge streamBridge;

    public JobEventPublisher(StreamBridge streamBridge) {
        this.streamBridge = streamBridge;
    }

    public void publish(JobPosted event) {
        boolean sent = streamBridge.send("jobPosted-out-0", event);
        log.info("Published JobPosted for job {} (sent={})", event.jobId(), sent);
    }

    public void publish(JobUpdated event) {
        boolean sent = streamBridge.send("jobUpdated-out-0", event);
        log.info("Published JobUpdated for job {} (sent={})", event.jobId(), sent);
    }

    public void publish(JobClosed event) {
        boolean sent = streamBridge.send("jobClosed-out-0", event);
        log.info("Published JobClosed for job {} (sent={})", event.jobId(), sent);
    }
}
