package com.campusgig.user.infrastructure.persistence;

import com.campusgig.user.domain.Availability;
import com.campusgig.user.domain.EmailAddress;
import com.campusgig.user.domain.Employer;
import com.campusgig.user.domain.Student;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
class UserRepositoriesTest {

    @Autowired
    private StudentRepository students;

    @Autowired
    private EmployerRepository employers;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void aStudentIsFoundByTheValueInsideTheirEmailValueObject() {
        students.save(new Student("Alex", new EmailAddress("alex@uowmail.edu.au"), Set.of(), Availability.FLEXIBLE));

        assertTrue(students.existsByEmailValue("alex@uowmail.edu.au"));
        assertFalse(students.existsByEmailValue("someone.else@uowmail.edu.au"));
    }

    @Test
    void aStudentsSkillsAreStoredAndReadBack() {
        Long id = students.save(new Student("Alex", new EmailAddress("alex@uowmail.edu.au"),
                Set.of("barista", "java"), Availability.WEEKENDS)).getId();
        entityManager.flush();
        entityManager.clear();

        Student reloaded = students.findById(id).orElseThrow();

        assertEquals(Set.of("barista", "java"), reloaded.getSkills());
        assertEquals(Availability.WEEKENDS, reloaded.getAvailability());
    }

    @Test
    void anEmployerIsFoundByEmail() {
        employers.save(new Employer("Northbeach Cafe", new EmailAddress("jobs@northbeachcafe.com.au")));

        assertTrue(employers.existsByEmailValue("jobs@northbeachcafe.com.au"));
        assertFalse(employers.existsByEmailValue("jobs@other.com.au"));
    }
}
