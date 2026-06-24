package com.melina.jobtrail.mapper;

import com.melina.jobtrail.dto.ApplicationDto;
import com.melina.jobtrail.dto.RequestApplicationDto;
import com.melina.jobtrail.entity.Application;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring", uses = {CompanyMapper.class})
public interface ApplicationMapper {

    ApplicationDto toDto(Application application);

    List<ApplicationDto> toDtoList(List<Application> applications);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "company", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateApplication(@MappingTarget Application application, RequestApplicationDto requestDto);
}
