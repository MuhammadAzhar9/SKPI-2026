package com.campus_commerce.order.exception;

public class CatalogServiceUnavailableException extends RuntimeException {
    public CatalogServiceUnavailableException(String message) {
        super(message);
    }
}
