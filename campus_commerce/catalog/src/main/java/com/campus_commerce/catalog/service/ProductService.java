package com.campus_commerce.catalog.service;

import com.campus_commerce.catalog.dto.request.CreateProductRequest;
import com.campus_commerce.catalog.dto.request.UpdateProductStatusRequest;
import com.campus_commerce.catalog.dto.request.UpdateStockRequest;
import com.campus_commerce.catalog.dto.response.PagedResponse;
import com.campus_commerce.catalog.dto.response.ProductResponse;
import com.campus_commerce.catalog.model.enums.ProductStatus;
import org.springframework.data.domain.Pageable;

public interface ProductService {
    ProductResponse createProduct(CreateProductRequest request);
    PagedResponse<ProductResponse> getProducts(String search, ProductStatus status, Pageable pageable);
    ProductResponse getProductById(Long id);
    ProductResponse updateStock(Long id, UpdateStockRequest request);
    ProductResponse updateStatus(Long id, UpdateProductStatusRequest request);
    void reduceStock(Long id, Integer quantity);
    void restoreStock(Long id, Integer quantity);
}
