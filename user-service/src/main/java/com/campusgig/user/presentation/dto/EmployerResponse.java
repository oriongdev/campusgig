package com.campusgig.user.presentation.dto;

import com.campusgig.user.domain.Employer;

public record EmployerResponse(Long id, String companyName, String email) {

    public static EmployerResponse from(Employer employer) {
        return new EmployerResponse(employer.getId(), employer.getCompanyName(), employer.getEmail().value());
    }
}
