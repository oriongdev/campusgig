package com.campusgig.matching.service;

import com.campusgig.matching.domain.JobSnapshot;
import com.campusgig.matching.domain.TrendingJob;
import com.campusgig.matching.domain.event.ApplicationSubmitted;
import com.campusgig.matching.domain.event.JobPosted;
import com.campusgig.matching.infrastructure.persistence.JobSnapshotRepository;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Comparator;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Real-time counts updated as events arrive.
 *
 * <p>This is the always-on version. When the application runs with the "streams" profile,
 * the same two queries are also computed by Kafka Streams (see infrastructure.streams),
 * and {@link AnalyticsQueryService} prefers those results.
 */
@Service
public class AnalyticsService {

    /** A job counts as trending on recent interest, not lifetime applications. */
    public static final Duration TRENDING_WINDOW = Duration.ofMinutes(15);
    public static final int TRENDING_THRESHOLD = 2;

    private final Map<String, AtomicLong> jobsPerCategory = new ConcurrentHashMap<>();
    private final Map<Long, AtomicLong> applicationsPerJob = new ConcurrentHashMap<>();
    private final Map<Long, Deque<Instant>> recentApplications = new ConcurrentHashMap<>();

    private final JobSnapshotRepository jobs;

    public AnalyticsService(JobSnapshotRepository jobs) {
        this.jobs = jobs;
    }

    public void recordJobPosted(JobPosted event) {
        jobsPerCategory.computeIfAbsent(event.category(), key -> new AtomicLong()).incrementAndGet();
    }

    public void recordApplicationSubmitted(ApplicationSubmitted event) {
        applicationsPerJob.computeIfAbsent(event.jobId(), key -> new AtomicLong()).incrementAndGet();
        Deque<Instant> times = recentApplications.computeIfAbsent(event.jobId(), key -> new ArrayDeque<>());
        synchronized (times) {
            times.addLast(event.occurredAt() == null ? Instant.now() : event.occurredAt());
        }
    }

    public Map<String, Long> jobsPerCategory() {
        return copyOf(jobsPerCategory);
    }

    public Map<Long, Long> applicationsPerJob() {
        return copyOf(applicationsPerJob);
    }

    /** Jobs with at least TRENDING_THRESHOLD applications inside the trending window. */
    public List<TrendingJob> trendingJobs() {
        Instant cutoff = Instant.now().minus(TRENDING_WINDOW);
        return recentApplications.entrySet().stream()
                .map(entry -> {
                    Deque<Instant> times = entry.getValue();
                    long count;
                    synchronized (times) {
                        while (!times.isEmpty() && times.peekFirst().isBefore(cutoff)) {
                            times.removeFirst();
                        }
                        count = times.size();
                    }
                    return new TrendingJob(entry.getKey(), titleOf(entry.getKey()), count);
                })
                .filter(job -> job.applicationsInWindow() >= TRENDING_THRESHOLD)
                .sorted(Comparator.comparingLong(TrendingJob::applicationsInWindow).reversed())
                .toList();
    }

    private String titleOf(Long jobId) {
        return jobs.findById(jobId).map(JobSnapshot::getTitle).orElse("job #" + jobId);
    }

    private static <K> Map<K, Long> copyOf(Map<K, AtomicLong> counters) {
        Map<K, Long> result = new TreeMap<>();
        counters.forEach((key, count) -> result.put(key, count.get()));
        return result;
    }
}
