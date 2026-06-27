package com.melina.jobtrail.mapper;

import com.melina.jobtrail.dto.CompanyResponse;
import com.melina.jobtrail.dto.CompanyRequest;
import com.melina.jobtrail.entity.Company;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring")
public interface CompanyMapper {
    CompanyResponse toResponse(Company company);

    List<CompanyResponse> toResponseList(List<Company> companies);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "user", ignore = true)
    void update(@MappingTarget Company company, CompanyRequest requestDto);
}
