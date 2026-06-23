package com.campus_commerce.catalog.exception;

public class InsufficientStockException extends RuntimeException {
    public InsufficientStockException(int available, int requested) {
        super("Insufficient stock. Available: " + available + ", requested: " + requested);
    }
}
