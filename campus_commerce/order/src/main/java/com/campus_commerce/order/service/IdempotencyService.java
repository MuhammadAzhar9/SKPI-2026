package com.campus_commerce.order.service;

import java.util.Optional;

public interface IdempotencyService {
    Optional<Long> findOrderId(String idempotencyKey);
    void save(String idempotencyKey, Long orderId);
}
