package com.melina.jobtrail.exception;

public class ProfileNotFoundException extends RuntimeException {
    public ProfileNotFoundException() {
        super("Profile not found");
    }
}
