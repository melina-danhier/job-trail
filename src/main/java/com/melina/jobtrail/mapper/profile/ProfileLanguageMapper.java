package com.melina.jobtrail.mapper.profile;

import com.melina.jobtrail.dto.profile.ProfileLanguageRequest;
import com.melina.jobtrail.entity.profile.ProfileLanguage;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ProfileLanguageMapper {
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "profile", ignore = true)
    ProfileLanguage toEntity(ProfileLanguageRequest language);

    List<ProfileLanguage> toEntityList(List<ProfileLanguageRequest> languages);
}
