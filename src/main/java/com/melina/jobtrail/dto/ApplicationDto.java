package com.melina.jobtrail.dto;

import com.melina.jobtrail.util.ApplicationStatus;

import java.time.Instant;
import java.time.LocalDate;

public record ApplicationDto(
        long id,
        CompanyDto company,
        String positionTitle,
        ApplicationStatus status,
        LocalDate applicationDate,
        String jobUrl,
        Instant createdAt,
        Instant updatedAt
) {
}
