package com.campusgig.application.presentation.dto;

import com.campusgig.application.domain.ApplicationStatus;

public record ChangeStatusRequest(ApplicationStatus status) {
}
