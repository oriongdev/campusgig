package com.campusgig.job.service;

import com.campusgig.job.domain.Job;
import com.campusgig.job.domain.JobStatus;
import com.campusgig.job.domain.Pay;
import com.campusgig.job.domain.PayType;
import com.campusgig.job.domain.event.JobClosed;
import com.campusgig.job.domain.event.JobPosted;
import com.campusgig.job.domain.event.JobUpdated;
import com.campusgig.job.infrastructure.messaging.JobEventPublisher;
import com.campusgig.job.infrastructure.persistence.JobRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Service
@Transactional
public class JobService {

    private final JobRepository jobs;
    private final JobEventPublisher events;

    public JobService(JobRepository jobs, JobEventPublisher events) {
        this.jobs = jobs;
        this.events = events;
    }

    public Job postJob(Long employerId, String title, String description, String category,
                       String location, BigDecimal payAmount, PayType payType) {
        Job job = jobs.save(new Job(employerId, title, description, category, location, new Pay(payAmount, payType)));
        events.publish(JobPosted.from(job));
        return job;
    }

    public Job updateJob(Long jobId, String title, String description, String category,
                         String location, BigDecimal payAmount, PayType payType) {
        Job job = getJob(jobId);
        job.update(title, description, category, location, new Pay(payAmount, payType));
        events.publish(JobUpdated.from(job));
        return job;
    }

    public Job closeJob(Long jobId) {
        Job job = getJob(jobId);
        job.close();
        events.publish(new JobClosed(job.getId(), Instant.now()));
        return job;
    }

    @Transactional(readOnly = true)
    public Job getJob(Long jobId) {
        return jobs.findById(jobId)
                .orElseThrow(() -> new NotFoundException("Job " + jobId + " not found"));
    }

    @Transactional(readOnly = true)
    public List<Job> searchOpenJobs(String category, String location, BigDecimal minPay) {
        return jobs.findByStatus(JobStatus.OPEN).stream()
                .filter(job -> job.matches(category, location, minPay))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<Job> jobsForEmployer(Long employerId) {
        return jobs.findByEmployerId(employerId);
    }
}
