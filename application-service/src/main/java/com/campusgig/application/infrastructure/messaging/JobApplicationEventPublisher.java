package com.campusgig.application.infrastructure.messaging;

import com.campusgig.application.domain.event.ApplicationStatusChanged;
import com.campusgig.application.domain.event.ApplicationSubmitted;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.stereotype.Component;

/**
 * Publishes application domain events to Kafka through Spring Cloud Stream.
 * Binding-to-topic mapping lives in application.yml.
 */
@Component
public class JobApplicationEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(JobApplicationEventPublisher.class);

    private final StreamBridge streamBridge;

    public JobApplicationEventPublisher(StreamBridge streamBridge) {
        this.streamBridge = streamBridge;
    }

    public void publish(ApplicationSubmitted event) {
        boolean sent = streamBridge.send("applicationSubmitted-out-0", event);
        log.info("Published ApplicationSubmitted for application {} (sent={})", event.applicationId(), sent);
    }

    public void publish(ApplicationStatusChanged event) {
        boolean sent = streamBridge.send("applicationStatusChanged-out-0", event);
        log.info("Published ApplicationStatusChanged for application {}: {} -> {} (sent={})",
                event.applicationId(), event.oldStatus(), event.newStatus(), sent);
    }
}
