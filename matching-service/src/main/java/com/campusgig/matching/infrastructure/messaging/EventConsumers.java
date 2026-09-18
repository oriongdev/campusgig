package com.campusgig.matching.infrastructure.messaging;

import com.campusgig.matching.domain.event.ApplicationStatusChanged;
import com.campusgig.matching.domain.event.ApplicationSubmitted;
import com.campusgig.matching.domain.event.JobClosed;
import com.campusgig.matching.domain.event.JobPosted;
import com.campusgig.matching.domain.event.JobUpdated;
import com.campusgig.matching.domain.event.StudentProfileUpdated;
import com.campusgig.matching.service.AnalyticsService;
import com.campusgig.matching.service.MatchingService;
import com.campusgig.matching.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.function.Consumer;

/**
 * Kafka consumers (Spring Cloud Stream functional style).
 * Each bean name must be listed in spring.cloud.function.definition in application.yml.
 */
@Configuration
public class EventConsumers {

    private static final Logger log = LoggerFactory.getLogger(EventConsumers.class);

    @Bean
    public Consumer<JobPosted> onJobPosted(MatchingService matching, AnalyticsService analytics) {
        return event -> {
            log.info("Received JobPosted for job {}", event.jobId());
            matching.recordJob(event);
            analytics.recordJobPosted(event);
        };
    }

    @Bean
    public Consumer<JobUpdated> onJobUpdated(MatchingService matching) {
        return event -> {
            log.info("Received JobUpdated for job {}", event.jobId());
            matching.updateJob(event);
        };
    }

    @Bean
    public Consumer<JobClosed> onJobClosed(MatchingService matching) {
        return event -> {
            log.info("Received JobClosed for job {}", event.jobId());
            matching.removeJob(event);
        };
    }

    @Bean
    public Consumer<StudentProfileUpdated> onStudentProfileUpdated(MatchingService matching) {
        return event -> {
            log.info("Received StudentProfileUpdated for student {}", event.studentId());
            matching.recordStudent(event);
        };
    }

    @Bean
    public Consumer<ApplicationSubmitted> onApplicationSubmitted(AnalyticsService analytics) {
        return event -> {
            log.info("Received ApplicationSubmitted for application {}", event.applicationId());
            analytics.recordApplicationSubmitted(event);
        };
    }

    @Bean
    public Consumer<ApplicationStatusChanged> onApplicationStatusChanged(NotificationService notifications) {
        return event -> {
            log.info("Received ApplicationStatusChanged for application {}: {} -> {}",
                    event.applicationId(), event.oldStatus(), event.newStatus());
            notifications.notifyStatusChange(event);
        };
    }
}
