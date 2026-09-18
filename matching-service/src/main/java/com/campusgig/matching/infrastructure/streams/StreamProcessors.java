package com.campusgig.matching.infrastructure.streams;

import com.campusgig.matching.domain.event.ApplicationSubmitted;
import com.campusgig.matching.domain.event.JobPosted;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.common.utils.Bytes;
import org.apache.kafka.streams.kstream.Grouped;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.Materialized;
import org.apache.kafka.streams.kstream.TimeWindows;
import org.apache.kafka.streams.state.KeyValueStore;
import org.apache.kafka.streams.state.WindowStore;
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
 * -- jobs per category
 * SELECT category, COUNT(*) FROM job_posted GROUP BY category;
 *
 * -- applications per job
 * SELECT jobId, COUNT(*) FROM application_submitted GROUP BY jobId;
 *
 * -- trending jobs (tumbling 15 minute window)
 * SELECT jobId, COUNT(*) FROM application_submitted
 * WINDOW TUMBLING (SIZE 15 MINUTES) GROUP BY jobId;
 * </pre>
 *
 * Results are kept in state stores and read back through interactive queries,
 * see {@link StreamAnalyticsService}.
 */
@Configuration
@Profile("streams")
public class StreamProcessors {

    public static final String JOBS_PER_CATEGORY_STORE = "jobs-per-category-store";
    public static final String APPLICATIONS_PER_JOB_STORE = "applications-per-job-store";
    public static final String TRENDING_JOBS_STORE = "trending-jobs-store";
    public static final Duration TRENDING_WINDOW = Duration.ofMinutes(15);

    @Bean
    public Consumer<KStream<String, JobPosted>> jobPostedStream() {
        return stream -> stream
                .filter((key, event) -> event != null && event.category() != null)
                .groupBy((key, event) -> event.category(),
                        Grouped.with(Serdes.String(), new JobPostedSerde()))
                .count(Materialized.<String, Long, KeyValueStore<Bytes, byte[]>>as(JOBS_PER_CATEGORY_STORE)
                        .withKeySerde(Serdes.String())
                        .withValueSerde(Serdes.Long()));
    }

    @Bean
    public Consumer<KStream<String, ApplicationSubmitted>> applicationSubmittedStream() {
        return stream -> {
            KStream<String, ApplicationSubmitted> byJob = stream
                    .filter((key, event) -> event != null && event.jobId() != null);

            byJob.groupBy((key, event) -> String.valueOf(event.jobId()),
                            Grouped.with(Serdes.String(), new ApplicationSubmittedSerde()))
                    .count(Materialized.<String, Long, KeyValueStore<Bytes, byte[]>>as(APPLICATIONS_PER_JOB_STORE)
                            .withKeySerde(Serdes.String())
                            .withValueSerde(Serdes.Long()));

            byJob.groupBy((key, event) -> String.valueOf(event.jobId()),
                            Grouped.with(Serdes.String(), new ApplicationSubmittedSerde()))
                    .windowedBy(TimeWindows.ofSizeWithNoGrace(TRENDING_WINDOW))
                    .count(Materialized.<String, Long, WindowStore<Bytes, byte[]>>as(TRENDING_JOBS_STORE)
                            .withKeySerde(Serdes.String())
                            .withValueSerde(Serdes.Long()));
        };
    }
}
