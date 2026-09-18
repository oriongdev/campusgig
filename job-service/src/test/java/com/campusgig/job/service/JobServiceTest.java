package com.campusgig.job.service;

import com.campusgig.job.domain.Job;
import com.campusgig.job.domain.JobStatus;
import com.campusgig.job.domain.PayType;
import com.campusgig.job.domain.event.JobClosed;
import com.campusgig.job.domain.event.JobPosted;
import com.campusgig.job.domain.event.JobUpdated;
import com.campusgig.job.infrastructure.messaging.JobEventPublisher;
import com.campusgig.job.infrastructure.persistence.JobRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JobServiceTest {

    private final JobRepository jobs = mock(JobRepository.class);
    private final JobEventPublisher events = mock(JobEventPublisher.class);
    private final JobService service = new JobService(jobs, events);

    private Job sampleJob() {
        return new Job(1L, "Weekend Barista", "Coffee", "hospitality", "Wollongong",
                new com.campusgig.job.domain.Pay(new BigDecimal("28.50"), PayType.HOURLY));
    }

    @Test
    void postingAJobPublishesJobPosted() {
        when(jobs.save(any(Job.class))).thenAnswer(call -> call.getArgument(0));

        service.postJob(1L, "Weekend Barista", "Coffee", "hospitality", "Wollongong",
                new BigDecimal("28.50"), PayType.HOURLY);

        verify(events).publish(any(JobPosted.class));
    }

    @Test
    void closingAJobPublishesJobClosed() {
        Job job = sampleJob();
        when(jobs.findById(1L)).thenReturn(Optional.of(job));

        Job closed = service.closeJob(1L);

        assertEquals(JobStatus.CLOSED, closed.getStatus());
        verify(events).publish(any(JobClosed.class));
    }

    @Test
    void editingAJobPublishesJobUpdated() {
        when(jobs.findById(1L)).thenReturn(Optional.of(sampleJob()));

        Job updated = service.updateJob(1L, "Barista (2 shifts)", "Weekends", "hospitality", "Wollongong",
                new BigDecimal("30.00"), PayType.HOURLY);

        assertEquals("Barista (2 shifts)", updated.getTitle());
        verify(events).publish(any(JobUpdated.class));
    }

    @Test
    void unknownJobIsReportedAsNotFound() {
        when(jobs.findById(99L)).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class, () -> service.getJob(99L));
    }

    @Test
    void searchOnlyReturnsMatchingOpenJobs() {
        when(jobs.findByStatus(JobStatus.OPEN)).thenReturn(List.of(sampleJob()));

        assertEquals(1, service.searchOpenJobs("hospitality", null, new BigDecimal("25")).size());
        assertEquals(0, service.searchOpenJobs("tutoring", null, null).size());
    }
}
