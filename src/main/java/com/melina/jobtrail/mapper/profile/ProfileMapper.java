package com.melina.jobtrail.mapper.profile;

import com.melina.jobtrail.dto.profile.ProfileRequest;
import com.melina.jobtrail.dto.profile.ProfileResponse;
import com.melina.jobtrail.entity.profile.Profile;
import org.mapstruct.AfterMapping;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring", uses = {
        ProfileLanguageMapper.class,
        ProfileProjectMapper.class,
        ProfileSkillMapper.class
}, builder = @Builder(disableBuilder = true))
public interface ProfileMapper {
    @Mapping(target = "id", ignore = true)
    Profile toEntity(ProfileRequest request);
    ProfileResponse toResponse(Profile profile);

    @Mapping(target = "id", ignore = true)
    void updateEntityFromRequest(@MappingTarget Profile profile, ProfileRequest request);

    @AfterMapping
    default void linkChildren(@MappingTarget Profile profile) {
        profile.getSkills().forEach(skill -> skill.setProfile(profile));
        profile.getLanguages().forEach(language -> language.setProfile(profile));
        profile.getProjects().forEach(project -> project.setProfile(profile));
    }
}
