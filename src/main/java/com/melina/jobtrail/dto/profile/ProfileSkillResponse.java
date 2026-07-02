package com.melina.jobtrail.dto.profile;

import com.melina.jobtrail.util.SkillLevel;

public record ProfileSkillResponse (
        String name,
        SkillLevel level,
        boolean mainSkill
) {
}
