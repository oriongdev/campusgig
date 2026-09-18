package com.campusgig.user.presentation.dto;

import com.campusgig.user.domain.Availability;

import java.util.Set;

public record RegisterStudentRequest(String name, String email, Set<String> skills, Availability availability) {
}
