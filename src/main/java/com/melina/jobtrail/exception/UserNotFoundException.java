package com.melina.jobtrail.exception;

import org.jspecify.annotations.NonNull;

public class UserNotFoundException extends RuntimeException {
    public UserNotFoundException(@NonNull String email) {
        super("User with email " + email + " not found");
    }
}
