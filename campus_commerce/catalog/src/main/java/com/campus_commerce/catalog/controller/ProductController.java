package com.campus_commerce.catalog.controller;

import com.campus_commerce.catalog.dto.request.CreateProductRequest;
import com.campus_commerce.catalog.dto.request.StockAdjustmentRequest;
import com.campus_commerce.catalog.dto.request.UpdateProductStatusRequest;
import com.campus_commerce.catalog.dto.request.UpdateStockRequest;
import com.campus_commerce.catalog.dto.response.PagedResponse;
import com.campus_commerce.catalog.dto.response.ProductResponse;
import com.campus_commerce.catalog.model.enums.ProductStatus;
import com.campus_commerce.catalog.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/products")
@Tag(name = "Products", description = "Manajemen produk — CRUD, stok, dan status")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @PostMapping
    @Operation(summary = "Buat produk baru", description = "Membuat produk baru dengan status default ACTIVE")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Produk berhasil dibuat"),
        @ApiResponse(responseCode = "400", description = "Validasi gagal"),
        @ApiResponse(responseCode = "409", description = "SKU sudah digunakan")
    })
    public ResponseEntity<ProductResponse> createProduct(@Valid @RequestBody CreateProductRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(productService.createProduct(request));
    }

    @GetMapping
    @Operation(summary = "Daftar produk", description = "Menampilkan daftar produk dengan dukungan pagination, search (nama/SKU), dan filter status")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Daftar produk berhasil diambil")
    })
    public ResponseEntity<PagedResponse<ProductResponse>> getAllProducts(
            @Parameter(description = "Cari berdasarkan nama atau SKU")
            @RequestParam(required = false) String search,
            @Parameter(description = "Filter berdasarkan status: ACTIVE atau INACTIVE")
            @RequestParam(required = false) ProductStatus status,
            @ParameterObject @PageableDefault(size = 10, sort = "name", direction = Sort.Direction.ASC) Pageable pageable) {
        return ResponseEntity.ok(productService.getProducts(search, status, pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Detail produk", description = "Menampilkan detail satu produk berdasarkan ID")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Produk ditemukan"),
        @ApiResponse(responseCode = "404", description = "Produk tidak ditemukan")
    })
    public ResponseEntity<ProductResponse> getProductById(@PathVariable Long id) {
        return ResponseEntity.ok(productService.getProductById(id));
    }

    @PatchMapping("/{id}/stock")
    @Operation(summary = "Update stok", description = "Menambah atau mengurangi stok produk secara manual")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Stok berhasil diperbarui"),
        @ApiResponse(responseCode = "400", description = "Stok tidak cukup atau validasi gagal"),
        @ApiResponse(responseCode = "404", description = "Produk tidak ditemukan")
    })
    public ResponseEntity<ProductResponse> updateStock(
            @PathVariable Long id,
            @Valid @RequestBody UpdateStockRequest request) {
        return ResponseEntity.ok(productService.updateStock(id, request));
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Ubah status produk", description = "Mengubah status produk menjadi ACTIVE atau INACTIVE")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Status berhasil diubah"),
        @ApiResponse(responseCode = "400", description = "Status tidak valid"),
        @ApiResponse(responseCode = "404", description = "Produk tidak ditemukan")
    })
    public ResponseEntity<ProductResponse> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateProductStatusRequest request) {
        return ResponseEntity.ok(productService.updateStatus(id, request));
    }

    @PatchMapping("/{id}/reduce-stock")
    @Operation(summary = "Kurangi stok (internal)", description = "Dipanggil oleh Order Service saat order dibuat")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Stok berhasil dikurangi"),
        @ApiResponse(responseCode = "400", description = "Stok tidak cukup atau produk INACTIVE"),
        @ApiResponse(responseCode = "404", description = "Produk tidak ditemukan")
    })
    public ResponseEntity<Void> reduceStock(
            @PathVariable Long id,
            @Valid @RequestBody StockAdjustmentRequest request) {
        productService.reduceStock(id, request.getQuantity());
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/{id}/restore-stock")
    @Operation(summary = "Kembalikan stok (internal)", description = "Dipanggil oleh Order Service saat order dibatalkan")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Stok berhasil dikembalikan"),
        @ApiResponse(responseCode = "404", description = "Produk tidak ditemukan")
    })
    public ResponseEntity<Void> restoreStock(
            @PathVariable Long id,
            @Valid @RequestBody StockAdjustmentRequest request) {
        productService.restoreStock(id, request.getQuantity());
        return ResponseEntity.ok().build();
    }
}
