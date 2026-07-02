package com.melina.jobtrail.exception;

public class ProfileAlreadyExistsException extends RuntimeException {
    public ProfileAlreadyExistsException() {
        super("Profile already exists");
    }
}
