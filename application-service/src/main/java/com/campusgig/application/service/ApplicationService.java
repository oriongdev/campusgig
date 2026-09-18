package com.campusgig.application.service;

import com.campusgig.application.domain.ApplicationStatus;
import com.campusgig.application.domain.JobApplication;
import com.campusgig.application.domain.event.ApplicationStatusChanged;
import com.campusgig.application.domain.event.ApplicationSubmitted;
import com.campusgig.application.infrastructure.client.JobServiceClient;
import com.campusgig.application.infrastructure.messaging.JobApplicationEventPublisher;
import com.campusgig.application.infrastructure.persistence.JobApplicationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@Transactional
public class ApplicationService {

    private final JobApplicationRepository applications;
    private final JobServiceClient jobServiceClient;
    private final JobApplicationEventPublisher events;

    public ApplicationService(JobApplicationRepository applications,
                              JobServiceClient jobServiceClient,
                              JobApplicationEventPublisher events) {
        this.applications = applications;
        this.jobServiceClient = jobServiceClient;
        this.events = events;
    }

    public JobApplication submit(Long jobId, Long studentId, String coverMessage) {
        if (jobId == null || studentId == null) {
            throw new IllegalArgumentException("jobId and studentId are required");
        }
        JobServiceClient.JobSummary job = jobServiceClient.findJob(jobId)
                .orElseThrow(() -> new NotFoundException("Job " + jobId + " not found"));
        if (!job.isOpen()) {
            throw new IllegalStateException("Job " + jobId + " is closed and not accepting applications");
        }
        if (applications.existsByJobIdAndStudentId(jobId, studentId)) {
            throw new IllegalStateException("Student " + studentId + " has already applied for job " + jobId);
        }
        JobApplication saved = applications.save(new JobApplication(jobId, studentId, coverMessage));
        events.publish(new ApplicationSubmitted(saved.getId(), saved.getJobId(), saved.getStudentId(), saved.getSubmittedAt()));
        return saved;
    }

    public JobApplication changeStatus(Long applicationId, ApplicationStatus newStatus) {
        JobApplication application = get(applicationId);
        ApplicationStatus previous = application.moveTo(newStatus);
        events.publish(new ApplicationStatusChanged(
                application.getId(),
                application.getJobId(),
                application.getStudentId(),
                previous.name(),
                application.getStatus().name(),
                Instant.now()));
        return application;
    }

    public JobApplication withdraw(Long applicationId) {
        return changeStatus(applicationId, ApplicationStatus.WITHDRAWN);
    }

    @Transactional(readOnly = true)
    public JobApplication get(Long applicationId) {
        return applications.findById(applicationId)
                .orElseThrow(() -> new NotFoundException("Application " + applicationId + " not found"));
    }

    @Transactional(readOnly = true)
    public List<JobApplication> forJob(Long jobId) {
        return applications.findByJobId(jobId);
    }

    @Transactional(readOnly = true)
    public List<JobApplication> forStudent(Long studentId) {
        return applications.findByStudentId(studentId);
    }
}
