package com.melina.jobtrail.dto;

import java.time.Instant;

public record UserDto(
        Long id,
        String email,
        Instant createdAt
) {
}
