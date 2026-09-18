package com.campusgig.user.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/**
 * Value object: an email address is defined only by its value and is immutable.
 */
@Embeddable
public record EmailAddress(@Column(name = "email", nullable = false) String value) {

    public EmailAddress {
        if (value == null) {
            throw new IllegalArgumentException("Email address is required");
        }
        value = value.trim().toLowerCase();
        if (!value.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
            throw new IllegalArgumentException("Invalid email address: " + value);
        }
    }
}
