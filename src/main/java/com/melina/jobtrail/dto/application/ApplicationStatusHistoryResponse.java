package com.melina.jobtrail.dto.application;

import com.melina.jobtrail.util.ApplicationStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "Historie eines Statuswechsels einer Bewerbung")
public record ApplicationStatusHistoryResponse(

        @Schema(description = "Eindeutige ID des Historie-Eintrags", example = "101")
        long id,

        @Schema(description = "Vorheriger Status", example = "APPLIED")
        ApplicationStatus previousStatus,

        @Schema(description = "Neuer Status nach dem Wechsel", example = "INTERVIEW")
        ApplicationStatus newStatus,

        @Schema(description = "Zeitpunkt der Statusänderung", example = "2026-10-17T11:30:00Z")
        Instant changedAt

) {
}
