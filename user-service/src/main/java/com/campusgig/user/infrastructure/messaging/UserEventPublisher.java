package com.campusgig.user.infrastructure.messaging;

import com.campusgig.user.domain.event.StudentProfileUpdated;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.stereotype.Component;

/**
 * Publishes user domain events to Kafka through Spring Cloud Stream.
 * Binding-to-topic mapping lives in application.yml.
 */
@Component
public class UserEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(UserEventPublisher.class);

    private final StreamBridge streamBridge;

    public UserEventPublisher(StreamBridge streamBridge) {
        this.streamBridge = streamBridge;
    }

    public void publish(StudentProfileUpdated event) {
        boolean sent = streamBridge.send("studentProfileUpdated-out-0", event);
        log.info("Published StudentProfileUpdated for student {} (sent={})", event.studentId(), sent);
    }
}
