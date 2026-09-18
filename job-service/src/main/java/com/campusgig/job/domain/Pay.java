package com.campusgig.job.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

import java.math.BigDecimal;

/**
 * Value object: how much a job pays and whether it is hourly or a fixed amount.
 */
@Embeddable
public record Pay(
        @Column(name = "pay_amount", nullable = false) BigDecimal amount,
        @Enumerated(EnumType.STRING) @Column(name = "pay_type", nullable = false) PayType type) {

    public Pay {
        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException("Pay amount must be greater than zero");
        }
        if (type == null) {
            throw new IllegalArgumentException("Pay type is required (HOURLY or FIXED)");
        }
    }
}
