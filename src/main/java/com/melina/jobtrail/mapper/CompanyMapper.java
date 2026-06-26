package com.melina.jobtrail.mapper;

import com.melina.jobtrail.dto.CompanyDto;
import com.melina.jobtrail.dto.RequestCompanyDto;
import com.melina.jobtrail.entity.Company;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring")
public interface CompanyMapper {
    CompanyDto toDto(Company company);

    List<CompanyDto> toDtoList(List<Company> companies);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "user", ignore = true)
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void update(@MappingTarget Company company, RequestCompanyDto requestDto);
}
