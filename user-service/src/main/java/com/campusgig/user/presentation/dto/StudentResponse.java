package com.campusgig.user.presentation.dto;

import com.campusgig.user.domain.Availability;
import com.campusgig.user.domain.Student;

import java.util.Set;

public record StudentResponse(Long id, String name, String email, Set<String> skills, Availability availability) {

    public static StudentResponse from(Student student) {
        return new StudentResponse(
                student.getId(),
                student.getName(),
                student.getEmail().value(),
                student.getSkills(),
                student.getAvailability());
    }
}
