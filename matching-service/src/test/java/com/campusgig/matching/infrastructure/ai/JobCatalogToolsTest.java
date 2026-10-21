package com.campusgig.matching.infrastructure.ai;

import com.campusgig.matching.domain.JobSnapshot;
import com.campusgig.matching.infrastructure.persistence.JobSnapshotRepository;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JobCatalogToolsTest {

    private final JobSnapshotRepository jobs = mock(JobSnapshotRepository.class);
    private final JobCatalogTools tools = new JobCatalogTools(jobs);

    private void catalogueHasTwoJobs() {
        when(jobs.findAll()).thenReturn(List.of(
                new JobSnapshot(1L, "Weekend Barista", "Make coffee", "hospitality", "Wollongong"),
                new JobSnapshot(2L, "Java Tutor", "Tutor first years", "tutoring", "UOW Campus")));
    }

    @Test
    void listOpenJobsDescribesEveryJobOnOneLineEach() {
        catalogueHasTwoJobs();

        String result = tools.listOpenJobs();

        assertEquals(2, result.lines().count());
        assertTrue(result.contains("jobId=1 | title=Weekend Barista | category=hospitality | location=Wollongong"));
        assertTrue(result.contains("jobId=2 | title=Java Tutor"));
    }

    @Test
    void findJobsByCategoryIgnoresCase() {
        catalogueHasTwoJobs();

        String result = tools.findJobsByCategory("TUTORING");

        assertTrue(result.contains("Java Tutor"));
        assertFalse(result.contains("Weekend Barista"));
    }

    @Test
    void anEmptyCatalogueIsReportedInWordsSoTheModelDoesNotInventJobs() {
        when(jobs.findAll()).thenReturn(List.of());

        assertEquals("No jobs available.", tools.listOpenJobs());
        assertEquals("No jobs available.", tools.findJobsByCategory("retail"));
    }
}
