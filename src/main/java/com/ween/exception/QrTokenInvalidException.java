package com.ween.exception;

public class QrTokenInvalidException extends RuntimeException {
    public QrTokenInvalidException(String message) {
        super(message);
    }

    public QrTokenInvalidException(String message, Throwable cause) {
        super(message, cause);
    }
}