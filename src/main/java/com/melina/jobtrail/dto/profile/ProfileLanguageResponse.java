package com.melina.jobtrail.dto.profile;

import com.melina.jobtrail.util.LanguageLevel;

public record ProfileLanguageResponse (
        String language,
        LanguageLevel level
) {
}
