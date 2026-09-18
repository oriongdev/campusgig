package com.campusgig.matching.service;

import com.campusgig.matching.domain.JobSnapshot;
import com.campusgig.matching.domain.Notification;
import com.campusgig.matching.domain.event.ApplicationStatusChanged;
import com.campusgig.matching.infrastructure.persistence.JobSnapshotRepository;
import com.campusgig.matching.infrastructure.persistence.NotificationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class NotificationService {

    private final NotificationRepository notifications;
    private final JobSnapshotRepository jobSnapshots;

    public NotificationService(NotificationRepository notifications, JobSnapshotRepository jobSnapshots) {
        this.notifications = notifications;
        this.jobSnapshots = jobSnapshots;
    }

    public void notifyStatusChange(ApplicationStatusChanged event) {
        String jobTitle = jobSnapshots.findById(event.jobId())
                .map(JobSnapshot::getTitle)
                .orElse("job #" + event.jobId());
        String message = "Your application for \"" + jobTitle + "\" is now " + event.newStatus();
        notifications.save(new Notification(event.studentId(), message));
    }

    @Transactional(readOnly = true)
    public List<Notification> forStudent(Long studentId) {
        return notifications.findByStudentIdOrderByCreatedAtDesc(studentId);
    }
}
