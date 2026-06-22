package com.melina.jobtrail.exception;

public class EmailAlreadyExistsException extends RuntimeException {
    public EmailAlreadyExistsException(String email) {
        super("User with Email " + email + " already exists");
    }
}
