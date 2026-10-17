package com.melina.jobtrail.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.data.domain.Page;

import java.util.List;

@Schema(description = "Generische Seitenantwort für paginierte Ergebnisse")
public record PageResponse<T>(

        @Schema(description = "Elemente der aktuellen Ergebnisseite")
        List<T> content,

        @Schema(description = "Aktuelle Seitennummer (0-basiert)", example = "0")
        int page,

        @Schema(description = "Anzahl der Elemente pro Seite", example = "20")
        int size,

        @Schema(description = "Gesamtanzahl aller Elemente über alle Seiten hinweg", example = "42")
        long totalElements,

        @Schema(description = "Gesamtzahl der verfügbaren Seiten", example = "3")
        int totalPages,

        @Schema(description = "Gibt an, ob dies die erste Seite ist", example = "true")
        boolean first,

        @Schema(description = "Gibt an, ob dies die letzte Seite ist", example = "false")
        boolean last

) {
    public static <T> PageResponse<T> from(Page<T> page) {
        return new PageResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isFirst(),
                page.isLast()
        );
    }
}
