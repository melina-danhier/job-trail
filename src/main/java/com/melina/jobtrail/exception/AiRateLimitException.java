package com.melina.jobtrail.exception;

public class AiRateLimitException extends RuntimeException {
    public AiRateLimitException() {
        super("AI job matching rate limit exceeded");
    }
}
