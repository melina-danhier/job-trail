package com.melina.jobtrail.exception;

public class DuplicateApplicationException extends RuntimeException {
    public DuplicateApplicationException() {
        super("An application for this company and position already exists");
    }
}
