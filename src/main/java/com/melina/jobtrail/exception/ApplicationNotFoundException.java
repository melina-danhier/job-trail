package com.melina.jobtrail.exception;

public class ApplicationNotFoundException extends RuntimeException {
    public ApplicationNotFoundException(long id) {
        super("Application with id " + id + " not found");
    }
}
