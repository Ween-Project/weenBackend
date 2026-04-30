package com.ween.exception;

public class RegistrationClosedException extends RuntimeException {
    public RegistrationClosedException(String message) {
        super(message);
    }
}
