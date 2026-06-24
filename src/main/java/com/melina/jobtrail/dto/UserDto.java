package com.melina.jobtrail.dto;

import java.time.Instant;

public record UserDto(
        String email,
        Instant createdAt
) {
}
