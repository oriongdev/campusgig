package com.campusgig.user.presentation.dto;

import com.campusgig.user.domain.Availability;

import java.util.Set;

public record UpdateProfileRequest(Set<String> skills, Availability availability) {
}
