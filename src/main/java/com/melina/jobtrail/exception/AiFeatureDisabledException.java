package com.melina.jobtrail.exception;

public class AiFeatureDisabledException extends RuntimeException {
    public AiFeatureDisabledException() {
        super("AI job matching is not configured");
    }
}
