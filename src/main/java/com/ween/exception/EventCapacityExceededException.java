package com.ween.exception;

public class EventCapacityExceededException extends RuntimeException {
    public EventCapacityExceededException(String message) {
        super(message);
    }

    public EventCapacityExceededException(String message, Throwable cause) {
        super(message, cause);
    }
}
