package com.melina.jobtrail.exception;

public class AiResponseParseException extends RuntimeException {
    public AiResponseParseException() {
        super("Failed to parse AI response");
    }
}
