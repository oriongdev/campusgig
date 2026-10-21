package com.campusgig.user.service;

import com.campusgig.user.domain.Availability;
import com.campusgig.user.domain.EmailAddress;
import com.campusgig.user.domain.Employer;
import com.campusgig.user.domain.Student;
import com.campusgig.user.domain.event.StudentProfileUpdated;
import com.campusgig.user.infrastructure.messaging.UserEventPublisher;
import com.campusgig.user.infrastructure.persistence.EmployerRepository;
import com.campusgig.user.infrastructure.persistence.StudentRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserServiceTest {

    private final StudentRepository students = mock(StudentRepository.class);
    private final EmployerRepository employers = mock(EmployerRepository.class);
    private final UserEventPublisher events = mock(UserEventPublisher.class);
    private final UserService service = new UserService(students, employers, events);

    private StudentProfileUpdated publishedEvent() {
        ArgumentCaptor<StudentProfileUpdated> published = ArgumentCaptor.forClass(StudentProfileUpdated.class);
        verify(events).publish(published.capture());
        return published.getValue();
    }

    @Test
    void registeringAStudentPublishesTheirProfile() {
        when(students.save(any(Student.class))).thenAnswer(call -> call.getArgument(0));

        Student saved = service.registerStudent("Alex Nguyen", "alex@uowmail.edu.au",
                Set.of("Barista", " Java "), Availability.WEEKENDS);

        assertEquals("Alex Nguyen", saved.getName());
        StudentProfileUpdated event = publishedEvent();
        assertEquals("Alex Nguyen", event.name());
        assertEquals(Set.of("barista", "java"), event.skills());
        assertEquals("WEEKENDS", event.availability());
    }

    @Test
    void anEmailCanOnlyBeRegisteredOnceWhateverItsCase() {
        when(students.existsByEmailValue("alex@uowmail.edu.au")).thenReturn(true);

        assertThrows(IllegalStateException.class, () -> service.registerStudent(
                "Alex", "  ALEX@uowmail.edu.au ", Set.of(), Availability.FLEXIBLE));
        verify(students, never()).save(any());
        verify(events, never()).publish(any());
    }

    @Test
    void anInvalidEmailIsRejectedBeforeAnythingIsSaved() {
        assertThrows(IllegalArgumentException.class, () -> service.registerStudent(
                "Alex", "not-an-email", Set.of(), Availability.FLEXIBLE));
        verify(students, never()).save(any());
    }

    @Test
    void updatingAProfilePublishesTheNewSkills() {
        Student alex = new Student("Alex", new EmailAddress("alex@uowmail.edu.au"), Set.of("barista"), Availability.WEEKENDS);
        when(students.findById(1L)).thenReturn(Optional.of(alex));

        service.updateStudentProfile(1L, Set.of("java", "tutoring"), Availability.EVENINGS);

        assertEquals(Set.of("java", "tutoring"), alex.getSkills());
        StudentProfileUpdated event = publishedEvent();
        assertEquals(Set.of("java", "tutoring"), event.skills());
        assertEquals("EVENINGS", event.availability());
    }

    @Test
    void updatingAnUnknownStudentIsNotFound() {
        when(students.findById(99L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> service.updateStudentProfile(99L, Set.of(), Availability.FLEXIBLE));
        verify(events, never()).publish(any());
    }

    @Test
    void registeringAnEmployerSavesIt() {
        when(employers.save(any(Employer.class))).thenAnswer(call -> call.getArgument(0));

        Employer saved = service.registerEmployer(" Northbeach Cafe ", "Jobs@NorthbeachCafe.com.au");

        assertEquals("Northbeach Cafe", saved.getCompanyName());
        assertEquals("jobs@northbeachcafe.com.au", saved.getEmail().value());
    }

    @Test
    void anEmployerEmailCanOnlyBeRegisteredOnce() {
        when(employers.existsByEmailValue("jobs@northbeachcafe.com.au")).thenReturn(true);

        assertThrows(IllegalStateException.class,
                () -> service.registerEmployer("Northbeach Cafe", "jobs@northbeachcafe.com.au"));
    }

    @Test
    void anUnknownEmployerIsNotFound() {
        when(employers.findById(5L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> service.getEmployer(5L));
    }
}
