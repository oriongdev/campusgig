package com.campusgig.user.domain;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StudentTest {

    @Test
    void emailIsTrimmedAndLowercased() {
        assertEquals("alex@uow.edu.au", new EmailAddress("  Alex@UOW.edu.au ").value());
    }

    @Test
    void invalidEmailIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new EmailAddress("not-an-email"));
        assertThrows(IllegalArgumentException.class, () -> new EmailAddress(null));
    }

    @Test
    void skillsAreNormalisedAndBlanksDropped() {
        Student student = new Student("Alex", new EmailAddress("alex@uow.edu.au"),
                Set.of(" Barista ", "JAVA", "  "), Availability.WEEKENDS);
        assertEquals(Set.of("barista", "java"), student.getSkills());
        assertEquals(Availability.WEEKENDS, student.getAvailability());
    }

    @Test
    void availabilityDefaultsToFlexible() {
        Student student = new Student("Alex", new EmailAddress("alex@uow.edu.au"), Set.of("java"), null);
        assertEquals(Availability.FLEXIBLE, student.getAvailability());
    }

    @Test
    void updatingProfileReplacesSkills() {
        Student student = new Student("Alex", new EmailAddress("alex@uow.edu.au"), Set.of("java"), Availability.EVENINGS);
        student.updateProfile(Set.of("tutoring"), Availability.WEEKDAYS);
        assertEquals(Set.of("tutoring"), student.getSkills());
        assertEquals(Availability.WEEKDAYS, student.getAvailability());
    }

    @Test
    void nameIsRequired() {
        assertThrows(IllegalArgumentException.class,
                () -> new Student(" ", new EmailAddress("alex@uow.edu.au"), Set.of(), null));
    }

    @Test
    void skillsCollectionIsNotModifiableFromOutside() {
        Student student = new Student("Alex", new EmailAddress("alex@uow.edu.au"), Set.of("java"), null);
        Set<String> skills = student.getSkills();
        assertThrows(UnsupportedOperationException.class, () -> skills.add("hacking"));
        assertTrue(student.getSkills().contains("java"));
    }
}
