package com.melina.jobtrail.exception;

public class AiResponseParseException extends RuntimeException {
    public AiResponseParseException() {
        super("Failed to parse AI response");
    }

    public AiResponseParseException(String message) {
        super(message);
    }

    public AiResponseParseException(String message, Throwable cause) {
        super(message, cause);
    }
}
