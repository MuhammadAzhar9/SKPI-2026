package com.campus_commerce.order.repository;

import com.campus_commerce.order.model.entity.Order;
import com.campus_commerce.order.model.enums.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<Order, Long> {
    Page<Order> findByStatus(OrderStatus status, Pageable pageable);
    Page<Order> findByCustomerEmailContainingIgnoreCase(String email, Pageable pageable);
    Page<Order> findByStatusAndCustomerEmailContainingIgnoreCase(OrderStatus status, String email, Pageable pageable);
    Page<Order> findByCustomerNameContainingIgnoreCase(String name, Pageable pageable);
    Page<Order> findByStatusAndCustomerNameContainingIgnoreCase(OrderStatus status, String name, Pageable pageable);
}
