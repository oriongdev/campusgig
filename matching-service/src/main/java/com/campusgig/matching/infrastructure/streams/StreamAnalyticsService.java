package com.campusgig.matching.infrastructure.streams;

import com.campusgig.matching.domain.JobSnapshot;
import com.campusgig.matching.domain.TrendingJob;
import com.campusgig.matching.infrastructure.persistence.JobSnapshotRepository;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.kstream.Windowed;
import org.apache.kafka.streams.state.KeyValueIterator;
import org.apache.kafka.streams.state.QueryableStoreTypes;
import org.apache.kafka.streams.state.ReadOnlyKeyValueStore;
import org.apache.kafka.streams.state.ReadOnlyWindowStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.stream.binder.kafka.streams.InteractiveQueryService;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

/**
 * Reads the Kafka Streams state stores through interactive queries.
 *
 * <p>Every method returns an empty Optional while its store is not queryable yet, which happens
 * for a few seconds after start-up while Kafka Streams is rebalancing. The REST layer then falls
 * back to the in-memory counters, so the endpoints never fail during start-up.
 */
@Service
@Profile("streams")
public class StreamAnalyticsService {

    private static final Logger log = LoggerFactory.getLogger(StreamAnalyticsService.class);

    private final InteractiveQueryService queries;
    private final JobSnapshotRepository jobs;

    public StreamAnalyticsService(InteractiveQueryService queries, JobSnapshotRepository jobs) {
        this.queries = queries;
        this.jobs = jobs;
    }

    /** Query 1: SELECT category, COUNT(*) FROM job_posted GROUP BY category. */
    public Optional<Map<String, Long>> jobsPerCategory() {
        try {
            ReadOnlyKeyValueStore<String, Long> store = queries.getQueryableStore(
                    StreamProcessors.JOBS_PER_CATEGORY_STORE, QueryableStoreTypes.keyValueStore());
            Map<String, Long> result = new TreeMap<>();
            try (KeyValueIterator<String, Long> all = store.all()) {
                all.forEachRemaining(entry -> result.put(entry.key, entry.value));
            }
            return Optional.of(result);
        } catch (Exception e) {
            return notReady(StreamProcessors.JOBS_PER_CATEGORY_STORE, e);
        }
    }

    /** Query 2a: SELECT jobId, COUNT(*) FROM application_submitted GROUP BY jobId. */
    public Optional<Map<Long, Long>> applicationsPerJob() {
        try {
            ReadOnlyKeyValueStore<String, Long> store = queries.getQueryableStore(
                    StreamProcessors.APPLICATIONS_PER_JOB_STORE, QueryableStoreTypes.keyValueStore());
            Map<Long, Long> result = new TreeMap<>();
            try (KeyValueIterator<String, Long> all = store.all()) {
                all.forEachRemaining(entry -> result.put(Long.valueOf(entry.key), entry.value));
            }
            return Optional.of(result);
        } catch (Exception e) {
            return notReady(StreamProcessors.APPLICATIONS_PER_JOB_STORE, e);
        }
    }

    /**
     * Query 2b: jobs with at least {@code threshold} applications in the current 15 minute window.
     * A window store holds one count per job per window, so this reads the windows that started
     * within the last 15 minutes and keeps the highest count for each job.
     */
    public Optional<List<TrendingJob>> trendingJobs(int threshold) {
        Map<Long, Long> counts = new HashMap<>();
        try {
            ReadOnlyWindowStore<String, Long> store = queries.getQueryableStore(
                    StreamProcessors.TRENDING_JOBS_STORE, QueryableStoreTypes.windowStore());
            Instant to = Instant.now();
            Instant from = to.minus(StreamProcessors.TRENDING_WINDOW);
            try (KeyValueIterator<Windowed<String>, Long> all = store.fetchAll(from, to)) {
                while (all.hasNext()) {
                    KeyValue<Windowed<String>, Long> entry = all.next();
                    counts.merge(Long.valueOf(entry.key.key()), entry.value, Math::max);
                }
            }
        } catch (Exception e) {
            return notReady(StreamProcessors.TRENDING_JOBS_STORE, e);
        }
        List<TrendingJob> trending = new ArrayList<>();
        counts.forEach((jobId, count) -> {
            if (count >= threshold) {
                String title = jobs.findById(jobId).map(JobSnapshot::getTitle).orElse("job #" + jobId);
                trending.add(new TrendingJob(jobId, title, count));
            }
        });
        trending.sort(Comparator.comparingLong(TrendingJob::applicationsInWindow).reversed());
        return Optional.of(trending);
    }

    /** True once all three state stores can be queried. */
    public boolean isReady() {
        return jobsPerCategory().isPresent() && applicationsPerJob().isPresent() && trendingJobs(1).isPresent();
    }

    private <T> Optional<T> notReady(String store, Exception e) {
        log.debug("State store {} is not ready yet: {}", store, e.getMessage());
        return Optional.empty();
    }
}
