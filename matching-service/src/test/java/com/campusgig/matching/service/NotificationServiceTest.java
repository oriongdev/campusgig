package com.campusgig.matching.service;

import com.campusgig.matching.domain.JobSnapshot;
import com.campusgig.matching.domain.Notification;
import com.campusgig.matching.domain.event.ApplicationStatusChanged;
import com.campusgig.matching.infrastructure.persistence.JobSnapshotRepository;
import com.campusgig.matching.infrastructure.persistence.NotificationRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NotificationServiceTest {

    private final NotificationRepository notifications = mock(NotificationRepository.class);
    private final JobSnapshotRepository jobs = mock(JobSnapshotRepository.class);
    private final NotificationService service = new NotificationService(notifications, jobs);

    private Notification savedNotification() {
        ArgumentCaptor<Notification> saved = ArgumentCaptor.forClass(Notification.class);
        verify(notifications).save(saved.capture());
        return saved.getValue();
    }

    @Test
    void aStatusChangeBecomesANotificationForThatStudentNamingTheJob() {
        when(jobs.findById(5L)).thenReturn(Optional.of(
                new JobSnapshot(5L, "Weekend Barista", "", "hospitality", "Wollongong")));

        service.notifyStatusChange(new ApplicationStatusChanged(1L, 5L, 7L, "SUBMITTED", "SHORTLISTED", Instant.now()));

        Notification notification = savedNotification();
        assertEquals(7L, notification.getStudentId());
        assertEquals("Your application for \"Weekend Barista\" is now SHORTLISTED", notification.getMessage());
    }

    @Test
    void ifTheJobEventHasNotArrivedYetTheNotificationUsesTheJobNumber() {
        when(jobs.findById(5L)).thenReturn(Optional.empty());

        service.notifyStatusChange(new ApplicationStatusChanged(1L, 5L, 7L, "SUBMITTED", "ACCEPTED", Instant.now()));

        assertEquals("Your application for \"job #5\" is now ACCEPTED", savedNotification().getMessage());
    }
}
