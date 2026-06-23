package com.campus_commerce.catalog.exception;

public class InvalidProductStatusException extends RuntimeException {
    public InvalidProductStatusException(String message) {
        super(message);
    }
}
