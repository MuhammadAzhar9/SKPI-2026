package com.campus_commerce.order.service.impl;

import com.campus_commerce.order.model.entity.IdempotencyRecord;
import com.campus_commerce.order.repository.IdempotencyRepository;
import com.campus_commerce.order.service.IdempotencyService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Slf4j
@Service
public class IdempotencyServiceImpl implements IdempotencyService {

    private final IdempotencyRepository idempotencyRepository;

    public IdempotencyServiceImpl(IdempotencyRepository idempotencyRepository) {
        this.idempotencyRepository = idempotencyRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Long> findOrderId(String idempotencyKey) {
        return idempotencyRepository.findByIdempotencyKey(idempotencyKey)
                .map(IdempotencyRecord::getOrderId);
    }

    @Override
    @Transactional
    public void save(String idempotencyKey, Long orderId) {
        IdempotencyRecord record = new IdempotencyRecord();
        record.setIdempotencyKey(idempotencyKey);
        record.setOrderId(orderId);
        idempotencyRepository.save(record);
        log.info("Idempotency key saved: key={}, orderId={}", idempotencyKey, orderId);
    }
}
