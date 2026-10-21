package com.campusgig.matching.infrastructure.streams;

import com.campusgig.matching.domain.event.ApplicationSubmitted;
import com.campusgig.matching.domain.event.JobPosted;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.kstream.Grouped;
import org.apache.kafka.streams.kstream.KGroupedStream;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.Materialized;
import org.apache.kafka.streams.kstream.TimeWindows;
import org.apache.kafka.streams.state.Stores;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.time.Duration;
import java.util.function.Consumer;

/**
 * Kafka Streams topologies for the two real-time analysis requirements.
 *
 * <p>In SQL-like terms the topologies compute:
 *
 * <pre>
 * -- Query 1: jobs per category (requirement 11)
 * SELECT category, COUNT(*) FROM job_posted GROUP BY category;
 *
 * -- Query 2a: applications per job (requirement 12)
 * SELECT jobId, COUNT(*) FROM application_submitted GROUP BY jobId;
 *
 * -- Query 2b: trending jobs (requirement 12), tumbling 15 minute window
 * SELECT jobId, COUNT(*) FROM application_submitted
 * WINDOW TUMBLING (SIZE 15 MINUTES) GROUP BY jobId;
 * </pre>
 *
 * <p>Each result is a KTable materialised in a state store and read back through interactive
 * queries, see {@link StreamAnalyticsService}.
 *
 * <p>The stores are in-memory with changelog topics switched off. Every service in this prototype
 * keeps its data in an in-memory H2 database that starts empty, so the counts start empty with
 * them and always describe the current run. A production system would use the default persistent
 * RocksDB stores with changelogs so the counts survive a restart.
 */
@Configuration
@Profile("streams")
public class StreamProcessors {

    public static final String JOBS_PER_CATEGORY_STORE = "jobs-per-category-store";
    public static final String APPLICATIONS_PER_JOB_STORE = "applications-per-job-store";
    public static final String TRENDING_JOBS_STORE = "trending-jobs-store";
    public static final Duration TRENDING_WINDOW = Duration.ofMinutes(15);

    /** How long closed windows stay queryable. */
    private static final Duration WINDOW_RETENTION = Duration.ofHours(1);

    @Bean
    public Consumer<KStream<String, JobPosted>> jobPostedStream() {
        return StreamProcessors::countJobsPerCategory;
    }

    @Bean
    public Consumer<KStream<String, ApplicationSubmitted>> applicationSubmittedStream() {
        return StreamProcessors::countApplicationsPerJob;
    }

    /** Query 1. Events arrive without a key, so they are re-keyed by category before counting. */
    static void countJobsPerCategory(KStream<String, JobPosted> jobPosted) {
        jobPosted
                .filter((key, event) -> event != null && event.category() != null)
                .groupBy((key, event) -> event.category(),
                        Grouped.with("jobs-by-category", Serdes.String(), new JobPostedSerde()))
                .count(Materialized.<String, Long>as(Stores.inMemoryKeyValueStore(JOBS_PER_CATEGORY_STORE))
                        .withKeySerde(Serdes.String())
                        .withValueSerde(Serdes.Long())
                        .withLoggingDisabled());
    }

    /** Queries 2a and 2b. Both aggregations share one stream that is re-keyed by job id. */
    static void countApplicationsPerJob(KStream<String, ApplicationSubmitted> applicationSubmitted) {
        KGroupedStream<String, ApplicationSubmitted> byJob = applicationSubmitted
                .filter((key, event) -> event != null && event.jobId() != null)
                .groupBy((key, event) -> String.valueOf(event.jobId()),
                        Grouped.with("applications-by-job", Serdes.String(), new ApplicationSubmittedSerde()));

        byJob.count(Materialized.<String, Long>as(Stores.inMemoryKeyValueStore(APPLICATIONS_PER_JOB_STORE))
                .withKeySerde(Serdes.String())
                .withValueSerde(Serdes.Long())
                .withLoggingDisabled());

        byJob.windowedBy(TimeWindows.ofSizeWithNoGrace(TRENDING_WINDOW))
                .count(Materialized.<String, Long>as(
                                Stores.inMemoryWindowStore(TRENDING_JOBS_STORE, WINDOW_RETENTION, TRENDING_WINDOW, false))
                        .withKeySerde(Serdes.String())
                        .withValueSerde(Serdes.Long())
                        .withLoggingDisabled());
    }
}
