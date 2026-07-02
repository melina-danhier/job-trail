package com.melina.jobtrail.mapper.profile;

import com.melina.jobtrail.dto.profile.ProfileProjectRequest;
import com.melina.jobtrail.entity.profile.ProfileProject;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ProfileProjectMapper {
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "profile", ignore = true)
    ProfileProject toEntity(ProfileProjectRequest project);

    List<ProfileProject> toEntityList(List<ProfileProjectRequest> projects);
}
