package com.campus_commerce.order.service;

import com.campus_commerce.order.client.CatalogClient;
import com.campus_commerce.order.client.dto.CatalogProductResponse;
import com.campus_commerce.order.dto.request.CreateOrderItemRequest;
import com.campus_commerce.order.dto.request.CreateOrderRequest;
import com.campus_commerce.order.dto.response.OrderResponse;
import com.campus_commerce.order.exception.CatalogServiceException;
import com.campus_commerce.order.exception.InvalidOrderStatusException;
import com.campus_commerce.order.exception.ProductInactiveException;
import com.campus_commerce.order.model.entity.Order;
import com.campus_commerce.order.model.entity.OrderItem;
import com.campus_commerce.order.model.enums.OrderStatus;
import com.campus_commerce.order.repository.OrderRepository;
import com.campus_commerce.order.service.impl.OrderServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private CatalogClient catalogClient;

    @InjectMocks
    private OrderServiceImpl orderService;

    private CatalogProductResponse activeProduct;
    private Order pendingOrder;

    @BeforeEach
    void setUp() {
        activeProduct = new CatalogProductResponse();
        activeProduct.setId(1L);
        activeProduct.setName("Test Product");
        activeProduct.setPrice(new BigDecimal("15000"));
        activeProduct.setStock(10);
        activeProduct.setStatus("ACTIVE");

        OrderItem item = new OrderItem();
        item.setProductId(1L);
        item.setProductName("Test Product");
        item.setUnitPrice(new BigDecimal("15000"));
        item.setQuantity(2);
        item.setSubtotal(new BigDecimal("30000"));

        pendingOrder = new Order();
        pendingOrder.setId(1L);
        pendingOrder.setCustomerName("Siti");
        pendingOrder.setCustomerEmail("siti@example.com");
        pendingOrder.setStatus(OrderStatus.PENDING);
        pendingOrder.setTotalAmount(new BigDecimal("30000"));
        pendingOrder.getItems().add(item);
        item.setOrder(pendingOrder);
    }

    @Test
    void createOrder_success_totalAndSnapshotCorrect() {
        CreateOrderItemRequest itemReq = new CreateOrderItemRequest();
        itemReq.setProductId(1L);
        itemReq.setQuantity(2);

        CreateOrderRequest request = new CreateOrderRequest();
        request.setCustomerName("Siti");
        request.setCustomerEmail("siti@example.com");
        request.setItems(List.of(itemReq));

        when(catalogClient.getProduct(1L)).thenReturn(activeProduct);
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> {
            Order o = inv.getArgument(0);
            o.setId(1L);
            return o;
        });

        OrderResponse response = orderService.createOrder(request);

        assertThat(response.getTotalAmount()).isEqualByComparingTo("30000");
        assertThat(response.getItems()).hasSize(1);
        assertThat(response.getItems().get(0).getProductName()).isEqualTo("Test Product");
        assertThat(response.getItems().get(0).getUnitPrice()).isEqualByComparingTo("15000");
        assertThat(response.getItems().get(0).getSubtotal()).isEqualByComparingTo("30000");
        verify(catalogClient).reduceStock(1L, 2);
    }

    @Test
    void createOrder_productNotFound_throwsException() {
        CreateOrderItemRequest itemReq = new CreateOrderItemRequest();
        itemReq.setProductId(99L);
        itemReq.setQuantity(1);

        CreateOrderRequest request = new CreateOrderRequest();
        request.setCustomerName("Siti");
        request.setCustomerEmail("siti@example.com");
        request.setItems(List.of(itemReq));

        when(catalogClient.getProduct(99L))
                .thenThrow(new CatalogServiceException("Product not found with id: 99"));

        assertThatThrownBy(() -> orderService.createOrder(request))
                .isInstanceOf(CatalogServiceException.class)
                .hasMessageContaining("not found");
        verify(catalogClient, never()).reduceStock(anyLong(), anyInt());
        verify(orderRepository, never()).save(any());
    }

    @Test
    void createOrder_inactiveProduct_throwsException() {
        activeProduct.setStatus("INACTIVE");

        CreateOrderItemRequest itemReq = new CreateOrderItemRequest();
        itemReq.setProductId(1L);
        itemReq.setQuantity(1);

        CreateOrderRequest request = new CreateOrderRequest();
        request.setCustomerName("Siti");
        request.setCustomerEmail("siti@example.com");
        request.setItems(List.of(itemReq));

        when(catalogClient.getProduct(1L)).thenReturn(activeProduct);

        assertThatThrownBy(() -> orderService.createOrder(request))
                .isInstanceOf(ProductInactiveException.class);
        verify(catalogClient, never()).reduceStock(anyLong(), anyInt());
        verify(orderRepository, never()).save(any());
    }

    @Test
    void createOrder_insufficientStock_throwsException() {
        activeProduct.setStock(1);

        CreateOrderItemRequest itemReq = new CreateOrderItemRequest();
        itemReq.setProductId(1L);
        itemReq.setQuantity(5);

        CreateOrderRequest request = new CreateOrderRequest();
        request.setCustomerName("Siti");
        request.setCustomerEmail("siti@example.com");
        request.setItems(List.of(itemReq));

        when(catalogClient.getProduct(1L)).thenReturn(activeProduct);

        assertThatThrownBy(() -> orderService.createOrder(request))
                .isInstanceOf(CatalogServiceException.class)
                .hasMessageContaining("Insufficient stock");
        verify(catalogClient, never()).reduceStock(anyLong(), anyInt());
        verify(orderRepository, never()).save(any());
    }

    @Test
    void payOrder_pending_success() {
        when(orderRepository.findById(1L)).thenReturn(Optional.of(pendingOrder));
        when(orderRepository.save(any())).thenReturn(pendingOrder);

        OrderResponse response = orderService.payOrder(1L);

        assertThat(response.getStatus()).isEqualTo(OrderStatus.PAID);
        verify(orderRepository).save(pendingOrder);
    }

    @Test
    void payOrder_nonPending_throwsException() {
        pendingOrder.setStatus(OrderStatus.PAID);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(pendingOrder));

        assertThatThrownBy(() -> orderService.payOrder(1L))
                .isInstanceOf(InvalidOrderStatusException.class);
        verify(orderRepository, never()).save(any());
    }

    @Test
    void cancelOrder_pending_success_restoresStock() {
        when(orderRepository.findById(1L)).thenReturn(Optional.of(pendingOrder));
        when(orderRepository.save(any())).thenReturn(pendingOrder);

        OrderResponse response = orderService.cancelOrder(1L);

        assertThat(response.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        verify(catalogClient).restoreStock(1L, 2);
        verify(orderRepository).save(pendingOrder);
    }

    @Test
    void cancelOrder_paid_throwsException() {
        pendingOrder.setStatus(OrderStatus.PAID);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(pendingOrder));

        assertThatThrownBy(() -> orderService.cancelOrder(1L))
                .isInstanceOf(InvalidOrderStatusException.class);
        verify(catalogClient, never()).restoreStock(anyLong(), anyInt());
        verify(orderRepository, never()).save(any());
    }

    @Test
    void cancelOrder_alreadyCancelled_throwsException() {
        pendingOrder.setStatus(OrderStatus.CANCELLED);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(pendingOrder));

        assertThatThrownBy(() -> orderService.cancelOrder(1L))
                .isInstanceOf(InvalidOrderStatusException.class);
        verify(catalogClient, never()).restoreStock(anyLong(), anyInt());
    }
}
