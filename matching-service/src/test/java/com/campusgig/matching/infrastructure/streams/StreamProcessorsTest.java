package com.campusgig.matching.infrastructure.streams;

import com.campusgig.matching.domain.event.ApplicationSubmitted;
import com.campusgig.matching.domain.event.JobPosted;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.kstream.Consumed;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks the shape of the Kafka Streams topologies and the serdes they read events with.
 * It needs no broker. The running behaviour is covered by the live check in demo.sh.
 */
class StreamProcessorsTest {

    private static String describeTopology() {
        StreamsBuilder builder = new StreamsBuilder();
        StreamProcessors.countJobsPerCategory(
                builder.stream("job-posted", Consumed.with(Serdes.String(), new JobPostedSerde())));
        StreamProcessors.countApplicationsPerJob(
                builder.stream("application-submitted", Consumed.with(Serdes.String(), new ApplicationSubmittedSerde())));
        Topology topology = builder.build();
        return topology.describe().toString();
    }

    @Test
    void theTopologyReadsBothEventTopics() {
        String topology = describeTopology();

        assertTrue(topology.contains("topics: [job-posted]"), topology);
        assertTrue(topology.contains("topics: [application-submitted]"), topology);
    }

    @Test
    void eachQueryIsMaterialisedInItsOwnQueryableStateStore() {
        String topology = describeTopology();

        assertTrue(topology.contains("stores: [" + StreamProcessors.JOBS_PER_CATEGORY_STORE + "]"), topology);
        assertTrue(topology.contains("stores: [" + StreamProcessors.APPLICATIONS_PER_JOB_STORE + "]"), topology);
        assertTrue(topology.contains("stores: [" + StreamProcessors.TRENDING_JOBS_STORE + "]"), topology);
    }

    @Test
    void eventsAreReKeyedThroughNamedRepartitionTopicsBeforeCounting() {
        String topology = describeTopology();

        assertTrue(topology.contains("jobs-by-category-repartition"), topology);
        assertTrue(topology.contains("applications-by-job-repartition"), topology);
        // Applications are re-keyed once and that one stream feeds both the total and the windowed count.
        assertEquals(1, topology.split("Sink: applications-by-job-repartition-sink", -1).length - 1, topology);
    }

    @Test
    void jobPostedEventsSurviveARoundTripThroughTheSerde() {
        JobPosted event = new JobPosted(1L, 2L, "Weekend Barista", "Make coffee", "hospitality", "Wollongong",
                new BigDecimal("28.50"), "HOURLY", Instant.parse("2026-10-07T01:02:03Z"));
        try (JobPostedSerde serde = new JobPostedSerde()) {
            byte[] bytes = serde.serializer().serialize("job-posted", event);

            assertEquals(event, serde.deserializer().deserialize("job-posted", bytes));
        }
    }

    @Test
    void theSerdeReadsTheJsonThatApplicationServicePublishes() {
        String published = """
                {"applicationId":4,"jobId":3,"studentId":2,"occurredAt":"2026-10-07T05:03:06.123456789Z"}
                """;
        try (ApplicationSubmittedSerde serde = new ApplicationSubmittedSerde()) {
            ApplicationSubmitted event = serde.deserializer().deserialize("application-submitted", published.getBytes());

            assertEquals(3L, event.jobId());
            assertEquals(Instant.parse("2026-10-07T05:03:06.123456789Z"), event.occurredAt());
        }
    }

    @Test
    void emptyMessagesAreSkippedInsteadOfCrashingTheStream() {
        try (JobPostedSerde serde = new JobPostedSerde()) {
            assertNull(serde.deserializer().deserialize("job-posted", new byte[0]));
            assertNull(serde.deserializer().deserialize("job-posted", null));
        }
    }
}
