package com.campusgig.application.infrastructure.persistence;

import com.campusgig.application.domain.JobApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
class JobApplicationRepositoryTest {

    @Autowired
    private JobApplicationRepository applications;

    @Test
    void savesAndFindsApplicationsByJob() {
        applications.save(new JobApplication(1L, 10L, "Keen"));
        applications.save(new JobApplication(1L, 11L, "Also keen"));
        applications.save(new JobApplication(2L, 10L, "Different job"));

        List<JobApplication> forJob = applications.findByJobId(1L);
        assertEquals(2, forJob.size());
    }

    @Test
    void findsApplicationsByStudent() {
        applications.save(new JobApplication(1L, 10L, ""));
        applications.save(new JobApplication(2L, 10L, ""));

        assertEquals(2, applications.findByStudentId(10L).size());
    }

    @Test
    void detectsDuplicateApplications() {
        applications.save(new JobApplication(1L, 10L, ""));

        assertTrue(applications.existsByJobIdAndStudentId(1L, 10L));
        assertFalse(applications.existsByJobIdAndStudentId(1L, 99L));
    }
}
