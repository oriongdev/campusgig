package com.campusgig.application.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JobApplicationTest {

    @Test
    void newApplicationStartsAsSubmitted() {
        JobApplication application = new JobApplication(1L, 2L, "I have barista experience");
        assertEquals(ApplicationStatus.SUBMITTED, application.getStatus());
    }

    @Test
    void canBeShortlistedThenAccepted() {
        JobApplication application = new JobApplication(1L, 2L, "");
        assertEquals(ApplicationStatus.SUBMITTED, application.moveTo(ApplicationStatus.SHORTLISTED));
        assertEquals(ApplicationStatus.SHORTLISTED, application.moveTo(ApplicationStatus.ACCEPTED));
        assertEquals(ApplicationStatus.ACCEPTED, application.getStatus());
    }

    @Test
    void finalStatusCannotChange() {
        JobApplication application = new JobApplication(1L, 2L, "");
        application.moveTo(ApplicationStatus.REJECTED);
        assertThrows(IllegalStateException.class, () -> application.moveTo(ApplicationStatus.ACCEPTED));
    }

    @Test
    void cannotGoBackToSubmitted() {
        JobApplication application = new JobApplication(1L, 2L, "");
        assertThrows(IllegalStateException.class, () -> application.moveTo(ApplicationStatus.SUBMITTED));
    }

    @Test
    void requiresJobAndStudent() {
        assertThrows(IllegalArgumentException.class, () -> new JobApplication(null, 2L, ""));
        assertThrows(IllegalArgumentException.class, () -> new JobApplication(1L, null, ""));
    }
}
