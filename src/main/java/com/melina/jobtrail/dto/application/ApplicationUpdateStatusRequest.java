package com.melina.jobtrail.dto.application;

import com.melina.jobtrail.util.ApplicationStatus;
import jakarta.validation.constraints.NotNull;

public record ApplicationUpdateStatusRequest(
        @NotNull(message = "Status cannot be null")
        ApplicationStatus status
) {
}
