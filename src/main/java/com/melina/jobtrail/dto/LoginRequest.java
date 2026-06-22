package com.melina.jobtrail.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(

        @NotBlank(message = "Email cannot be null or blank")
        @Email(message = "Invalid email format")
        @Size(max = 255, message = "Email must not exceed 255 characters")
        String email,

        @NotBlank(message = "Password cannot be null or blank")
        @Size(min = 8, max = 50, message = "Password must be between 8 and 50 characters")
        String password

) {
}
