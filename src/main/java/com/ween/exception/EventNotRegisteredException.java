package com.ween.exception;

public class EventNotRegisteredException extends RuntimeException {
    public EventNotRegisteredException(String message) {
        super(message);
    }

    public EventNotRegisteredException(String message, Throwable cause) {
        super(message, cause);
    }
}
