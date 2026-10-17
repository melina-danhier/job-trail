package com.melina.jobtrail.dto.application;

import com.melina.jobtrail.util.ApplicationStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Anfragedaten zur Aktualisierung des Bewerbungsstatus")
public record ApplicationUpdateStatusRequest(

        @Schema(description = "Neuer Status der Bewerbung (SAVED, APPLIED, UNDER_REVIEW, INTERVIEW_SCHEDULED, INTERVIEW_COMPLETED, OFFER_RECEIVED, REJECTED, ACCEPTED, WITHDRAWN)", example = "INTERVIEW_SCHEDULED")
        @NotNull(message = "Status cannot be null")
        ApplicationStatus status

) {
}
