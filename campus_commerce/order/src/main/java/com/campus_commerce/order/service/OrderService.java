package com.campus_commerce.order.service;

import com.campus_commerce.order.dto.request.CreateOrderRequest;
import com.campus_commerce.order.dto.response.OrderResponse;
import com.campus_commerce.order.dto.response.PagedResponse;
import com.campus_commerce.order.model.enums.OrderStatus;
import org.springframework.data.domain.Pageable;

public interface OrderService {
    OrderResponse createOrder(CreateOrderRequest request);
    PagedResponse<OrderResponse> getOrders(OrderStatus status, String customerEmail, Pageable pageable);
    OrderResponse getOrderById(Long id);
    OrderResponse payOrder(Long id);
    OrderResponse cancelOrder(Long id);
}
