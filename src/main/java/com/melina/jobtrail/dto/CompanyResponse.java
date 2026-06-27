package com.melina.jobtrail.dto;

public record CompanyResponse(
        long id,
        String name,
        String website,
        String location
) {
}
