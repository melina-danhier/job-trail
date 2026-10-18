package com.melina.jobtrail.util;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Handlungsempfehlung für den Bewerber auf Basis der KI-Passungsanalyse")
public enum JobMatchRecommendation {
    @Schema(description = "Bewerbung wird empfohlen (sehr gute Passung der Kernanforderungen)")
    APPLY,

    @Schema(description = "Bewerbung eventuell sinnvoll (teilweise Passung oder geringfügige Lücken)")
    MAYBE,

    @Schema(description = "Bewerbung wird nicht empfohlen (starke Diskrepanz in Kernkompetenzen oder Ausschlusskriterien)")
    DO_NOT_APPLY
}
