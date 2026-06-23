package com.campus_commerce.order.client;

import com.campus_commerce.order.client.dto.CatalogProductResponse;
import com.campus_commerce.order.exception.CatalogServiceException;
import com.campus_commerce.order.exception.CatalogServiceUnavailableException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Slf4j
@Component
public class CatalogClient {

    private final RestClient restClient;

    public CatalogClient(RestClient catalogRestClient) {
        this.restClient = catalogRestClient;
    }

    public CatalogProductResponse getProduct(Long productId) {
        try {
            return restClient.get()
                    .uri("/api/products/{id}", productId)
                    .retrieve()
                    .onStatus(status -> status.value() == 404, (req, res) -> {
                        throw new CatalogServiceException("Product not found with id: " + productId);
                    })
                    .onStatus(HttpStatusCode::isError, (req, res) -> {
                        throw new CatalogServiceException("Catalog Service returned error: " + res.getStatusCode().value());
                    })
                    .body(CatalogProductResponse.class);
        } catch (CatalogServiceException e) {
            log.warn("Catalog error on getProduct productId={}: {}", productId, e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Catalog Service unreachable on getProduct productId={}: {}", productId, e.getMessage());
            throw new CatalogServiceUnavailableException("Catalog Service is unavailable");
        }
    }

    public void reduceStock(Long productId, Integer quantity) {
        try {
            restClient.patch()
                    .uri("/api/products/{id}/reduce-stock", productId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("quantity", quantity))
                    .retrieve()
                    .onStatus(status -> status.value() == 400, (req, res) -> {
                        throw new CatalogServiceException("Insufficient stock for product id: " + productId);
                    })
                    .onStatus(HttpStatusCode::isError, (req, res) -> {
                        throw new CatalogServiceException("Catalog Service returned error: " + res.getStatusCode().value());
                    })
                    .toBodilessEntity();
        } catch (CatalogServiceException e) {
            log.warn("Catalog error on reduceStock productId={}, quantity={}: {}", productId, quantity, e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Catalog Service unreachable on reduceStock productId={}: {}", productId, e.getMessage());
            throw new CatalogServiceUnavailableException("Catalog Service is unavailable");
        }
    }

    public void restoreStock(Long productId, Integer quantity) {
        try {
            restClient.patch()
                    .uri("/api/products/{id}/restore-stock", productId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("quantity", quantity))
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (req, res) -> {
                        throw new CatalogServiceException("Catalog Service returned error: " + res.getStatusCode().value());
                    })
                    .toBodilessEntity();
        } catch (CatalogServiceException e) {
            log.warn("Catalog error on restoreStock productId={}, quantity={}: {}", productId, quantity, e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Catalog Service unreachable on restoreStock productId={}: {}", productId, e.getMessage());
            throw new CatalogServiceUnavailableException("Catalog Service is unavailable");
        }
    }
}
