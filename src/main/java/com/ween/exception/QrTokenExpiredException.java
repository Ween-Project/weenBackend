package com.ween.exception;

public class QrTokenExpiredException extends RuntimeException {
    public QrTokenExpiredException(String message) {
        super(message);
    }

    public QrTokenExpiredException(String message, Throwable cause) {
        super(message, cause);
    }
}