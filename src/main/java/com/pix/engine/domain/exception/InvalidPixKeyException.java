package com.pix.engine.domain.exception;

public class InvalidPixKeyException extends RuntimeException {

    public InvalidPixKeyException(String pixKey) {
        super("Invalid or not found Pix key: " + pixKey);
    }
}
