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
import java.util.TreeMap;

/**
 * Reads the Kafka Streams state stores through interactive queries.
 * Returns empty results while the streams are still starting up, so the REST layer can fall back.
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

    public Map<String, Long> jobsPerCategory() {
        Map<String, Long> result = new TreeMap<>();
        try {
            ReadOnlyKeyValueStore<String, Long> store =
                    queries.getQueryableStore(StreamProcessors.JOBS_PER_CATEGORY_STORE, QueryableStoreTypes.keyValueStore());
            try (KeyValueIterator<String, Long> all = store.all()) {
                while (all.hasNext()) {
                    KeyValue<String, Long> entry = all.next();
                    result.put(entry.key, entry.value);
                }
            }
        } catch (Exception e) {
            log.debug("jobs-per-category store not ready yet: {}", e.getMessage());
        }
        return result;
    }

    public Map<Long, Long> applicationsPerJob() {
        Map<Long, Long> result = new TreeMap<>();
        try {
            ReadOnlyKeyValueStore<String, Long> store =
                    queries.getQueryableStore(StreamProcessors.APPLICATIONS_PER_JOB_STORE, QueryableStoreTypes.keyValueStore());
            try (KeyValueIterator<String, Long> all = store.all()) {
                while (all.hasNext()) {
                    KeyValue<String, Long> entry = all.next();
                    result.put(Long.valueOf(entry.key), entry.value);
                }
            }
        } catch (Exception e) {
            log.debug("applications-per-job store not ready yet: {}", e.getMessage());
        }
        return result;
    }

    public List<TrendingJob> trendingJobs(int threshold) {
        Map<Long, Long> counts = new HashMap<>();
        try {
            ReadOnlyWindowStore<String, Long> store =
                    queries.getQueryableStore(StreamProcessors.TRENDING_JOBS_STORE, QueryableStoreTypes.windowStore());
            Instant to = Instant.now();
            Instant from = to.minus(StreamProcessors.TRENDING_WINDOW);
            try (KeyValueIterator<Windowed<String>, Long> all = store.fetchAll(from, to)) {
                while (all.hasNext()) {
                    KeyValue<Windowed<String>, Long> entry = all.next();
                    Long jobId = Long.valueOf(entry.key.key());
                    counts.merge(jobId, entry.value, Math::max);
                }
            }
        } catch (Exception e) {
            log.debug("trending-jobs store not ready yet: {}", e.getMessage());
        }
        List<TrendingJob> trending = new ArrayList<>();
        counts.forEach((jobId, count) -> {
            if (count >= threshold) {
                String title = jobs.findById(jobId).map(JobSnapshot::getTitle).orElse("job #" + jobId);
                trending.add(new TrendingJob(jobId, title, count));
            }
        });
        trending.sort(Comparator.comparingLong(TrendingJob::applicationsInWindow).reversed());
        return trending;
    }
}
