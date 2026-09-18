package com.campusgig.user.service;

import com.campusgig.user.domain.Availability;
import com.campusgig.user.domain.EmailAddress;
import com.campusgig.user.domain.Employer;
import com.campusgig.user.domain.Student;
import com.campusgig.user.domain.event.StudentProfileUpdated;
import com.campusgig.user.infrastructure.messaging.UserEventPublisher;
import com.campusgig.user.infrastructure.persistence.EmployerRepository;
import com.campusgig.user.infrastructure.persistence.StudentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Service
@Transactional
public class UserService {

    private final StudentRepository students;
    private final EmployerRepository employers;
    private final UserEventPublisher events;

    public UserService(StudentRepository students, EmployerRepository employers, UserEventPublisher events) {
        this.students = students;
        this.employers = employers;
        this.events = events;
    }

    public Student registerStudent(String name, String email, Set<String> skills, Availability availability) {
        EmailAddress address = new EmailAddress(email);
        if (students.existsByEmailValue(address.value())) {
            throw new IllegalStateException("A student with this email already exists");
        }
        Student saved = students.save(new Student(name, address, skills, availability));
        events.publish(StudentProfileUpdated.from(saved));
        return saved;
    }

    public Student updateStudentProfile(Long studentId, Set<String> skills, Availability availability) {
        Student student = getStudent(studentId);
        student.updateProfile(skills, availability);
        events.publish(StudentProfileUpdated.from(student));
        return student;
    }

    @Transactional(readOnly = true)
    public Student getStudent(Long studentId) {
        return students.findById(studentId)
                .orElseThrow(() -> new NotFoundException("Student " + studentId + " not found"));
    }

    @Transactional(readOnly = true)
    public List<Student> listStudents() {
        return students.findAll();
    }

    public Employer registerEmployer(String companyName, String email) {
        EmailAddress address = new EmailAddress(email);
        if (employers.existsByEmailValue(address.value())) {
            throw new IllegalStateException("An employer with this email already exists");
        }
        return employers.save(new Employer(companyName, address));
    }

    @Transactional(readOnly = true)
    public List<Employer> listEmployers() {
        return employers.findAll();
    }

    @Transactional(readOnly = true)
    public Employer getEmployer(Long employerId) {
        return employers.findById(employerId)
                .orElseThrow(() -> new NotFoundException("Employer " + employerId + " not found"));
    }
}
