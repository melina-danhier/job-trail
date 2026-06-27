package com.melina.jobtrail.mapper;

import com.melina.jobtrail.dto.application.ApplicationRequest;
import com.melina.jobtrail.dto.application.ApplicationResponse;
import com.melina.jobtrail.entity.Application;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring", uses = {CompanyMapper.class})
public interface ApplicationMapper {

    ApplicationResponse toResponse(Application application);

    List<ApplicationResponse> toResponseList(List<Application> applications);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "company", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateApplication(@MappingTarget Application application, ApplicationRequest requestDto);
}
