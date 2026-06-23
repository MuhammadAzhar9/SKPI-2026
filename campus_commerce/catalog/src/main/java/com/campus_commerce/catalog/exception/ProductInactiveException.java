package com.campus_commerce.catalog.exception;

public class ProductInactiveException extends RuntimeException {
    public ProductInactiveException(Long id) {
        super("Product with id " + id + " is inactive");
    }
}
