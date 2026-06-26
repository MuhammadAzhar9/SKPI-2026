package com.campus_commerce.catalog.service;

import com.campus_commerce.catalog.dto.request.CreateProductRequest;
import com.campus_commerce.catalog.dto.response.ProductResponse;
import com.campus_commerce.catalog.exception.DuplicateSkuException;
import com.campus_commerce.catalog.exception.InsufficientStockException;
import com.campus_commerce.catalog.exception.ProductInactiveException;
import com.campus_commerce.catalog.exception.ProductNotFoundException;
import com.campus_commerce.catalog.model.entity.Product;
import com.campus_commerce.catalog.model.enums.ProductStatus;
import com.campus_commerce.catalog.repository.ProductRepository;
import com.campus_commerce.catalog.service.impl.ProductServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceImplTest {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ProductServiceImpl productService;

    private Product activeProduct;

    @BeforeEach
    void setUp() {
        activeProduct = new Product();
        activeProduct.setId(1L);
        activeProduct.setSku("SKU-001");
        activeProduct.setName("Test Product");
        activeProduct.setPrice(new BigDecimal("10000"));
        activeProduct.setStock(10);
        activeProduct.setStatus(ProductStatus.ACTIVE);
    }

    @Test
    void createProduct_success() {
        CreateProductRequest request = new CreateProductRequest();
        request.setSku("SKU-001");
        request.setName("Test Product");
        request.setPrice(new BigDecimal("10000"));
        request.setStock(10);

        when(productRepository.existsBySku("SKU-001")).thenReturn(false);
        when(productRepository.save(any(Product.class))).thenReturn(activeProduct);

        ProductResponse response = productService.createProduct(request);

        assertThat(response.getSku()).isEqualTo("SKU-001");
        assertThat(response.getName()).isEqualTo("Test Product");
        assertThat(response.getPrice()).isEqualByComparingTo("10000");
        verify(productRepository).save(any(Product.class));
    }

    @Test
    void createProduct_duplicateSku_throwsException() {
        CreateProductRequest request = new CreateProductRequest();
        request.setSku("SKU-001");

        when(productRepository.existsBySku("SKU-001")).thenReturn(true);

        assertThatThrownBy(() -> productService.createProduct(request))
                .isInstanceOf(DuplicateSkuException.class);
        verify(productRepository, never()).save(any());
    }

    @Test
    void getProductById_notFound_throwsException() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.getProductById(99L))
                .isInstanceOf(ProductNotFoundException.class);
    }

    @Test
    void reduceStock_success() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(activeProduct));
        when(productRepository.save(any())).thenReturn(activeProduct);

        productService.reduceStock(1L, 3);

        assertThat(activeProduct.getStock()).isEqualTo(7);
        verify(productRepository).save(activeProduct);
    }

    @Test
    void reduceStock_insufficientStock_throwsException() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(activeProduct));

        assertThatThrownBy(() -> productService.reduceStock(1L, 20))
                .isInstanceOf(InsufficientStockException.class);
        verify(productRepository, never()).save(any());
    }

    @Test
    void reduceStock_inactiveProduct_throwsException() {
        activeProduct.setStatus(ProductStatus.INACTIVE);
        when(productRepository.findById(1L)).thenReturn(Optional.of(activeProduct));

        assertThatThrownBy(() -> productService.reduceStock(1L, 1))
                .isInstanceOf(ProductInactiveException.class);
        verify(productRepository, never()).save(any());
    }

    @Test
    void restoreStock_success() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(activeProduct));
        when(productRepository.save(any())).thenReturn(activeProduct);

        productService.restoreStock(1L, 5);

        assertThat(activeProduct.getStock()).isEqualTo(15);
        verify(productRepository).save(activeProduct);
    }
}
