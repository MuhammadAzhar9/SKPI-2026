package com.campus_commerce.order.controller;

import com.campus_commerce.order.dto.request.CreateOrderRequest;
import com.campus_commerce.order.dto.response.OrderResponse;
import com.campus_commerce.order.dto.response.PagedResponse;
import com.campus_commerce.order.model.enums.OrderStatus;
import com.campus_commerce.order.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/orders")
@Tag(name = "Orders", description = "Manajemen order — buat, bayar, dan batalkan order")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    @Operation(summary = "Buat order baru", description = "Membuat order baru dengan status PENDING. Stok produk berkurang otomatis.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Order berhasil dibuat"),
        @ApiResponse(responseCode = "400", description = "Validasi gagal, produk INACTIVE, atau stok tidak cukup"),
        @ApiResponse(responseCode = "503", description = "Catalog Service tidak tersedia")
    })
    public ResponseEntity<OrderResponse> createOrder(@Valid @RequestBody CreateOrderRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(orderService.createOrder(request));
    }

    @GetMapping
    @Operation(summary = "Daftar order", description = "Menampilkan daftar order dengan pagination, filter status, dan filter email")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Daftar order berhasil diambil")
    })
    public ResponseEntity<PagedResponse<OrderResponse>> getAllOrders(
            @Parameter(description = "Filter berdasarkan status: PENDING, PAID, atau CANCELLED")
            @RequestParam(required = false) OrderStatus status,
            @Parameter(description = "Filter berdasarkan email customer (partial match)")
            @RequestParam(required = false) String customerEmail,
            @ParameterObject @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(orderService.getOrders(status, customerEmail, pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Detail order", description = "Menampilkan detail order beserta seluruh item dan snapshot produk")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Order ditemukan"),
        @ApiResponse(responseCode = "404", description = "Order tidak ditemukan")
    })
    public ResponseEntity<OrderResponse> getOrderById(@PathVariable Long id) {
        return ResponseEntity.ok(orderService.getOrderById(id));
    }

    @PatchMapping("/{id}/pay")
    @Operation(summary = "Bayar order", description = "Mengubah status order dari PENDING menjadi PAID")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Order berhasil dibayar"),
        @ApiResponse(responseCode = "404", description = "Order tidak ditemukan"),
        @ApiResponse(responseCode = "409", description = "Order bukan dalam status PENDING")
    })
    public ResponseEntity<OrderResponse> payOrder(@PathVariable Long id) {
        return ResponseEntity.ok(orderService.payOrder(id));
    }

    @PatchMapping("/{id}/cancel")
    @Operation(summary = "Batalkan order", description = "Mengubah status order dari PENDING menjadi CANCELLED. Stok dikembalikan otomatis.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Order berhasil dibatalkan"),
        @ApiResponse(responseCode = "404", description = "Order tidak ditemukan"),
        @ApiResponse(responseCode = "409", description = "Order bukan dalam status PENDING")
    })
    public ResponseEntity<OrderResponse> cancelOrder(@PathVariable Long id) {
        return ResponseEntity.ok(orderService.cancelOrder(id));
    }
}
