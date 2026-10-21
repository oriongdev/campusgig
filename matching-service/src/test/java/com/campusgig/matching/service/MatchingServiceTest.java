package com.campusgig.matching.service;

import com.campusgig.matching.domain.JobRecommendation;
import com.campusgig.matching.domain.JobSnapshot;
import com.campusgig.matching.domain.StudentSnapshot;
import com.campusgig.matching.domain.event.JobClosed;
import com.campusgig.matching.domain.event.JobPosted;
import com.campusgig.matching.domain.event.JobUpdated;
import com.campusgig.matching.domain.event.StudentProfileUpdated;
import com.campusgig.matching.infrastructure.persistence.JobSnapshotRepository;
import com.campusgig.matching.infrastructure.persistence.StudentSnapshotRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** The read models that matching-service builds from other services' events. */
class MatchingServiceTest {

    private final JobSnapshotRepository jobs = mock(JobSnapshotRepository.class);
    private final StudentSnapshotRepository students = mock(StudentSnapshotRepository.class);
    private final MatchingService service = new MatchingService(jobs, students);

    @Test
    void jobPostedStoresALocalCopyOfTheJob() {
        service.recordJob(new JobPosted(1L, 9L, "Weekend Barista", "Make coffee", "hospitality", "Wollongong",
                new BigDecimal("28.50"), "HOURLY", Instant.now()));

        ArgumentCaptor<JobSnapshot> saved = ArgumentCaptor.forClass(JobSnapshot.class);
        verify(jobs).save(saved.capture());
        assertEquals(1L, saved.getValue().getJobId());
        assertEquals("Weekend Barista", saved.getValue().getTitle());
        assertEquals("hospitality", saved.getValue().getCategory());
    }

    @Test
    void jobUpdatedReplacesTheLocalCopy() {
        service.updateJob(new JobUpdated(1L, "Senior Barista", "Run the machine", "hospitality", "Thirroul",
                new BigDecimal("32.00"), "HOURLY", Instant.now()));

        ArgumentCaptor<JobSnapshot> saved = ArgumentCaptor.forClass(JobSnapshot.class);
        verify(jobs).save(saved.capture());
        assertEquals("Senior Barista", saved.getValue().getTitle());
        assertEquals("Thirroul", saved.getValue().getLocation());
    }

    @Test
    void jobClosedRemovesTheLocalCopySoClosedJobsAreNeverRecommended() {
        service.removeJob(new JobClosed(1L, Instant.now()));

        verify(jobs).deleteById(1L);
    }

    @Test
    void aStudentsFirstProfileEventCreatesTheirSnapshot() {
        when(students.findById(3L)).thenReturn(Optional.empty());

        service.recordStudent(new StudentProfileUpdated(3L, "Alex", Set.of("barista"), "WEEKENDS", Instant.now()));

        ArgumentCaptor<StudentSnapshot> saved = ArgumentCaptor.forClass(StudentSnapshot.class);
        verify(students).save(saved.capture());
        assertEquals(3L, saved.getValue().getStudentId());
        assertEquals(Set.of("barista"), saved.getValue().getSkills());
    }

    @Test
    void aLaterProfileEventReplacesTheSkills() {
        StudentSnapshot existing = new StudentSnapshot(3L);
        existing.update("Alex", Set.of("barista"), "WEEKENDS");
        when(students.findById(3L)).thenReturn(Optional.of(existing));

        service.recordStudent(new StudentProfileUpdated(3L, "Alex", Set.of("java", "tutoring"), "EVENINGS", Instant.now()));

        assertEquals(Set.of("java", "tutoring"), existing.getSkills());
        assertEquals("EVENINGS", existing.getAvailability());
    }

    @Test
    void recommendationsAreRankedByHowManySkillsMatch() {
        StudentSnapshot alex = new StudentSnapshot(3L);
        alex.update("Alex", Set.of("barista", "customer service"), "WEEKENDS");
        when(students.findById(3L)).thenReturn(Optional.of(alex));
        when(jobs.findAll()).thenReturn(List.of(
                new JobSnapshot(1L, "Shop Assistant", "Customer service on the floor", "retail", "Wollongong"),
                new JobSnapshot(2L, "Weekend Barista", "Coffee and customer service", "hospitality", "Wollongong"),
                new JobSnapshot(3L, "Java Tutor", "Tutor first years", "tutoring", "UOW Campus")));

        List<JobRecommendation> recommendations = service.recommendJobs(3L);

        assertEquals(List.of(2L, 1L), recommendations.stream().map(JobRecommendation::jobId).toList());
        assertEquals(1.0, recommendations.get(0).score());
        assertEquals(0.5, recommendations.get(1).score());
    }

    @Test
    void recommendationsNeedAProfileEventFirst() {
        when(students.findById(42L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> service.recommendJobs(42L));
    }
}
