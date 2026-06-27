package com.melina.jobtrail.dto;

import java.time.Instant;

public record UserResponse(
        String email,
        Instant createdAt
) {
}
