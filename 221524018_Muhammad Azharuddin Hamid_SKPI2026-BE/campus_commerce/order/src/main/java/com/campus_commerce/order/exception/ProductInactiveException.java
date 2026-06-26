package com.campus_commerce.order.exception;

public class ProductInactiveException extends RuntimeException {
    public ProductInactiveException(Long productId) {
        super("Product with id " + productId + " is not available for ordering");
    }
}
