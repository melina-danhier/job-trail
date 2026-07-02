package com.melina.jobtrail.dto.profile;

import java.util.Set;

public record ProfileProjectResponse (
        String name,
        String description,
        Set<String> technologies
) {
}
