package com.campusgig.user.domain;

import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

/**
 * Aggregate root for an employer account.
 */
@Entity
public class Employer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String companyName;

    @Embedded
    private EmailAddress email;

    protected Employer() {
        // for JPA
    }

    public Employer(String companyName, EmailAddress email) {
        if (companyName == null || companyName.isBlank()) {
            throw new IllegalArgumentException("Company name is required");
        }
        if (email == null) {
            throw new IllegalArgumentException("Employer email is required");
        }
        this.companyName = companyName.trim();
        this.email = email;
    }

    public Long getId() {
        return id;
    }

    public String getCompanyName() {
        return companyName;
    }

    public EmailAddress getEmail() {
        return email;
    }
}
