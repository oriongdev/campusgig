package com.campusgig.job.presentation.dto;

import com.campusgig.job.domain.PayType;

import java.math.BigDecimal;

public record PostJobRequest(
        Long employerId,
        String title,
        String description,
        String category,
        String location,
        BigDecimal payAmount,
        PayType payType) {
}
