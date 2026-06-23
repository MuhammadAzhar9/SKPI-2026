package com.campus_commerce.catalog.service.impl;

import com.campus_commerce.catalog.dto.request.CreateProductRequest;
import com.campus_commerce.catalog.dto.request.UpdateProductStatusRequest;
import com.campus_commerce.catalog.dto.request.UpdateStockRequest;
import com.campus_commerce.catalog.dto.response.PagedResponse;
import com.campus_commerce.catalog.dto.response.ProductResponse;
import com.campus_commerce.catalog.exception.DuplicateSkuException;
import com.campus_commerce.catalog.exception.InsufficientStockException;
import com.campus_commerce.catalog.exception.ProductInactiveException;
import com.campus_commerce.catalog.exception.ProductNotFoundException;
import com.campus_commerce.catalog.model.entity.Product;
import com.campus_commerce.catalog.model.enums.ProductStatus;
import com.campus_commerce.catalog.model.enums.StockOperation;
import com.campus_commerce.catalog.repository.ProductRepository;
import com.campus_commerce.catalog.service.ProductService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;

    public ProductServiceImpl(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Override
    @Transactional
    public ProductResponse createProduct(CreateProductRequest request) {
        if (productRepository.existsBySku(request.getSku())) {
            throw new DuplicateSkuException(request.getSku());
        }

        Product product = new Product();
        product.setSku(request.getSku());
        product.setName(request.getName());
        product.setPrice(request.getPrice());
        product.setStock(request.getStock());

        return ProductResponse.from(productRepository.save(product));
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<ProductResponse> getProducts(String search, ProductStatus status, Pageable pageable) {
        Page<Product> page;

        boolean hasSearch = search != null && !search.isBlank();
        boolean hasStatus = status != null;

        if (hasSearch && hasStatus) {
            page = productRepository.findByNameOrSkuContainingAndStatus(search, status, pageable);
        } else if (hasSearch) {
            page = productRepository.findByNameOrSkuContaining(search, pageable);
        } else if (hasStatus) {
            page = productRepository.findByStatus(status, pageable);
        } else {
            page = productRepository.findAll(pageable);
        }

        return PagedResponse.from(page.map(ProductResponse::from));
    }

    @Override
    @Transactional(readOnly = true)
    public ProductResponse getProductById(Long id) {
        return ProductResponse.from(findById(id));
    }

    @Override
    @Transactional
    public ProductResponse updateStock(Long id, UpdateStockRequest request) {
        Product product = findById(id);

        if (request.getOperation() == StockOperation.DECREASE) {
            if (product.getStock() < request.getQuantity()) {
                throw new InsufficientStockException(product.getStock(), request.getQuantity());
            }
            product.setStock(product.getStock() - request.getQuantity());
        } else {
            product.setStock(product.getStock() + request.getQuantity());
        }

        return ProductResponse.from(productRepository.save(product));
    }

    @Override
    @Transactional
    public ProductResponse updateStatus(Long id, UpdateProductStatusRequest request) {
        Product product = findById(id);
        product.setStatus(request.getStatus());
        return ProductResponse.from(productRepository.save(product));
    }

    @Override
    @Transactional
    public void reduceStock(Long id, Integer quantity) {
        Product product = findById(id);
        if (product.getStatus() == ProductStatus.INACTIVE) {
            throw new ProductInactiveException(id);
        }
        if (product.getStock() < quantity) {
            throw new InsufficientStockException(product.getStock(), quantity);
        }
        product.setStock(product.getStock() - quantity);
        productRepository.save(product);
    }

    @Override
    @Transactional
    public void restoreStock(Long id, Integer quantity) {
        Product product = findById(id);
        product.setStock(product.getStock() + quantity);
        productRepository.save(product);
    }

    private Product findById(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(id));
    }
}
