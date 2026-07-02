package com.melina.jobtrail.mapper.profile;

import com.melina.jobtrail.dto.profile.ProfileSkillRequest;
import com.melina.jobtrail.entity.profile.ProfileSkill;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ProfileSkillMapper {
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "profile", ignore = true)
    ProfileSkill toEntity(ProfileSkillRequest skill);

    List<ProfileSkill> toEntityList(List<ProfileSkillRequest> skills);
}
