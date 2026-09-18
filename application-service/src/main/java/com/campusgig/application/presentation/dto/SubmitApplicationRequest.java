package com.campusgig.application.presentation.dto;

public record SubmitApplicationRequest(Long jobId, Long studentId, String coverMessage) {
}
