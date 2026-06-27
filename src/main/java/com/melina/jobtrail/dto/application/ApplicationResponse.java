package com.melina.jobtrail.dto.application;

import com.melina.jobtrail.dto.CompanyResponse;
import com.melina.jobtrail.util.ApplicationStatus;

import java.time.Instant;
import java.time.LocalDate;

public record ApplicationResponse(
        long id,
        CompanyResponse company,
        String positionTitle,
        ApplicationStatus status,
        LocalDate applicationDate,
        String jobUrl,
        Instant createdAt,
        Instant updatedAt
) {
}
