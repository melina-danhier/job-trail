package com.melina.jobtrail.dto.application;

import com.melina.jobtrail.util.ApplicationStatus;

import java.time.Instant;

public record ApplicationStatusHistoryResponse(
        long id,
        ApplicationStatus previousStatus,
        ApplicationStatus newStatus,
        Instant changedAt
) {
}
