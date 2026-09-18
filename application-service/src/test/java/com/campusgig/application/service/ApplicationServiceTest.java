package com.campusgig.application.service;

import com.campusgig.application.domain.ApplicationStatus;
import com.campusgig.application.domain.JobApplication;
import com.campusgig.application.domain.event.ApplicationStatusChanged;
import com.campusgig.application.domain.event.ApplicationSubmitted;
import com.campusgig.application.infrastructure.client.JobServiceClient;
import com.campusgig.application.infrastructure.messaging.JobApplicationEventPublisher;
import com.campusgig.application.infrastructure.persistence.JobApplicationRepository;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ApplicationServiceTest {

    private final JobApplicationRepository applications = mock(JobApplicationRepository.class);
    private final JobServiceClient jobServiceClient = mock(JobServiceClient.class);
    private final JobApplicationEventPublisher events = mock(JobApplicationEventPublisher.class);
    private final ApplicationService service = new ApplicationService(applications, jobServiceClient, events);

    private void jobIs(String status) {
        when(jobServiceClient.findJob(1L))
                .thenReturn(Optional.of(new JobServiceClient.JobSummary(1L, "Weekend Barista", status)));
    }

    @Test
    void applyingToAnOpenJobPublishesApplicationSubmitted() {
        jobIs("OPEN");
        when(applications.existsByJobIdAndStudentId(1L, 2L)).thenReturn(false);
        when(applications.save(any(JobApplication.class))).thenAnswer(call -> call.getArgument(0));

        service.submit(1L, 2L, "Keen to help");

        verify(events).publish(any(ApplicationSubmitted.class));
    }

    @Test
    void applyingToAClosedJobIsRejected() {
        jobIs("CLOSED");
        assertThrows(IllegalStateException.class, () -> service.submit(1L, 2L, ""));
    }

    @Test
    void applyingTwiceIsRejected() {
        jobIs("OPEN");
        when(applications.existsByJobIdAndStudentId(1L, 2L)).thenReturn(true);
        assertThrows(IllegalStateException.class, () -> service.submit(1L, 2L, ""));
    }

    @Test
    void applyingToAnUnknownJobIsNotFound() {
        when(jobServiceClient.findJob(9L)).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class, () -> service.submit(9L, 2L, ""));
    }

    @Test
    void statusChangePublishesEventWithOldAndNewStatus() {
        JobApplication application = new JobApplication(1L, 2L, "");
        when(applications.findById(5L)).thenReturn(Optional.of(application));

        service.changeStatus(5L, ApplicationStatus.SHORTLISTED);

        assertEquals(ApplicationStatus.SHORTLISTED, application.getStatus());
        verify(events).publish(any(ApplicationStatusChanged.class));
    }
}
