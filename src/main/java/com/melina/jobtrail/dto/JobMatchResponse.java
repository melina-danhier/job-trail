package com.melina.jobtrail.dto;

import java.util.List;

public record JobMatchResponse (
        int score,
        List<String> matchingSkills,
        List<String> missingSkills,
        String recommendation,
        String summary
) {

}
