package com.campus_commerce.order.service.impl;

import com.campus_commerce.order.client.CatalogClient;
import com.campus_commerce.order.client.dto.CatalogProductResponse;
import com.campus_commerce.order.dto.request.CreateOrderItemRequest;
import com.campus_commerce.order.dto.request.CreateOrderRequest;
import com.campus_commerce.order.dto.response.OrderResponse;
import com.campus_commerce.order.dto.response.PagedResponse;
import com.campus_commerce.order.exception.CatalogServiceException;
import com.campus_commerce.order.exception.InvalidOrderStatusException;
import com.campus_commerce.order.exception.OrderNotFoundException;
import com.campus_commerce.order.exception.ProductInactiveException;
import com.campus_commerce.order.model.entity.Order;
import com.campus_commerce.order.model.entity.OrderItem;
import com.campus_commerce.order.model.enums.OrderStatus;
import com.campus_commerce.order.repository.OrderRepository;
import com.campus_commerce.order.service.OrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final CatalogClient catalogClient;

    public OrderServiceImpl(OrderRepository orderRepository, CatalogClient catalogClient) {
        this.orderRepository = orderRepository;
        this.catalogClient = catalogClient;
    }

    @Override
    @Transactional
    public OrderResponse createOrder(CreateOrderRequest request) {
        List<CreateOrderItemRequest> itemRequests = request.getItems();

        // Step 1: Validate all products before touching stock
        List<CatalogProductResponse> products = new ArrayList<>();
        for (CreateOrderItemRequest itemReq : itemRequests) {
            CatalogProductResponse product = catalogClient.getProduct(itemReq.getProductId());

            if (!"ACTIVE".equals(product.getStatus())) {
                throw new ProductInactiveException(itemReq.getProductId());
            }

            if (product.getStock() < itemReq.getQuantity()) {
                throw new CatalogServiceException(
                        "Insufficient stock for product id: " + itemReq.getProductId() +
                        ". Available: " + product.getStock() + ", requested: " + itemReq.getQuantity());
            }

            products.add(product);
        }

        // Step 2: Reduce stock for all items
        for (int i = 0; i < itemRequests.size(); i++) {
            catalogClient.reduceStock(itemRequests.get(i).getProductId(), itemRequests.get(i).getQuantity());
        }

        // Step 3: Build order with snapshot data
        Order order = new Order();
        order.setCustomerName(request.getCustomerName());
        order.setCustomerEmail(request.getCustomerEmail());

        BigDecimal totalAmount = BigDecimal.ZERO;
        List<OrderItem> items = new ArrayList<>();

        for (int i = 0; i < itemRequests.size(); i++) {
            CatalogProductResponse product = products.get(i);
            Integer quantity = itemRequests.get(i).getQuantity();

            BigDecimal subtotal = product.getPrice().multiply(BigDecimal.valueOf(quantity));

            OrderItem item = new OrderItem();
            item.setOrder(order);
            item.setProductId(product.getId());
            item.setProductName(product.getName());
            item.setUnitPrice(product.getPrice());
            item.setQuantity(quantity);
            item.setSubtotal(subtotal);

            items.add(item);
            totalAmount = totalAmount.add(subtotal);
        }

        order.setItems(items);
        order.setTotalAmount(totalAmount);

        Order saved = orderRepository.save(order);
        log.info("Order created: id={}, customer={}, totalAmount={}, itemCount={}",
                saved.getId(), saved.getCustomerName(), saved.getTotalAmount(), saved.getItems().size());
        return OrderResponse.from(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<OrderResponse> getOrders(OrderStatus status, String customerEmail, Pageable pageable) {
        Page<Order> page;

        boolean hasStatus = status != null;
        boolean hasEmail = customerEmail != null && !customerEmail.isBlank();

        if (hasStatus && hasEmail) {
            page = orderRepository.findByStatusAndCustomerEmailContainingIgnoreCase(status, customerEmail, pageable);
        } else if (hasStatus) {
            page = orderRepository.findByStatus(status, pageable);
        } else if (hasEmail) {
            page = orderRepository.findByCustomerEmailContainingIgnoreCase(customerEmail, pageable);
        } else {
            page = orderRepository.findAll(pageable);
        }

        return PagedResponse.from(page.map(OrderResponse::from));
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponse getOrderById(Long id) {
        return OrderResponse.from(findById(id));
    }

    @Override
    @Transactional
    public OrderResponse payOrder(Long id) {
        Order order = findById(id);
        if (order.getStatus() != OrderStatus.PENDING) {
            throw new InvalidOrderStatusException(
                    "Only PENDING orders can be paid. Current status: " + order.getStatus());
        }
        order.setStatus(OrderStatus.PAID);
        log.info("Order paid: id={}", id);
        return OrderResponse.from(orderRepository.save(order));
    }

    @Override
    @Transactional
    public OrderResponse cancelOrder(Long id) {
        Order order = findById(id);
        if (order.getStatus() != OrderStatus.PENDING) {
            throw new InvalidOrderStatusException(
                    "Only PENDING orders can be cancelled. Current status: " + order.getStatus());
        }

        for (OrderItem item : order.getItems()) {
            catalogClient.restoreStock(item.getProductId(), item.getQuantity());
        }

        order.setStatus(OrderStatus.CANCELLED);
        log.info("Order cancelled: id={}, stockRestoredForItems={}", id, order.getItems().size());
        return OrderResponse.from(orderRepository.save(order));
    }

    private Order findById(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new OrderNotFoundException(id));
    }
}
