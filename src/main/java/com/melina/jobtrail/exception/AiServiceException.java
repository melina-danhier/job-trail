package com.melina.jobtrail.exception;

public class AiServiceException extends RuntimeException {
    public AiServiceException(Throwable cause) {
        super("AI service is temporarily unavailable", cause);
    }
}
