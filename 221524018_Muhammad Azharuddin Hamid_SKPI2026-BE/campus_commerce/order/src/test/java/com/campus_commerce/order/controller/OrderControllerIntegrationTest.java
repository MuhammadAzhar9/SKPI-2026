package com.campus_commerce.order.controller;

import com.campus_commerce.order.client.CatalogClient;
import com.campus_commerce.order.client.dto.CatalogProductResponse;
import com.campus_commerce.order.exception.CatalogServiceException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:ordertestdb;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.show-sql=false",
        "catalog.service.base-url=http://localhost:8081"
})
class OrderControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CatalogClient catalogClient;

    private CatalogProductResponse activeProduct;

    @BeforeEach
    void setUp() {
        activeProduct = new CatalogProductResponse();
        activeProduct.setId(1L);
        activeProduct.setName("Mie Goreng");
        activeProduct.setPrice(new BigDecimal("12000"));
        activeProduct.setStock(20);
        activeProduct.setStatus("ACTIVE");

        when(catalogClient.getProduct(anyLong())).thenReturn(activeProduct);
        doNothing().when(catalogClient).reduceStock(anyLong(), anyInt());
        doNothing().when(catalogClient).restoreStock(anyLong(), anyInt());
    }

    private String orderJson(String name, String email, long productId, int qty) throws Exception {
        return objectMapper.writeValueAsString(Map.of(
                "customerName", name,
                "customerEmail", email,
                "items", List.of(Map.of("productId", productId, "quantity", qty))
        ));
    }

    private long createOrder() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(orderJson("Siti", "siti@example.com", 1L, 2)))
                .andExpect(status().isCreated())
                .andReturn();
        return com.jayway.jsonpath.JsonPath.parse(result.getResponse().getContentAsString())
                .read("$.id", Long.class);
    }

    @Test
    void createOrder_validRequest_returns201WithSnapshotAndTotal() throws Exception {
        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(orderJson("Budi", "budi@example.com", 1L, 3)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.status", is("PENDING")))
                .andExpect(jsonPath("$.totalAmount", is(36000)))
                .andExpect(jsonPath("$.items[0].productName", is("Mie Goreng")))
                .andExpect(jsonPath("$.items[0].unitPrice", is(12000)))
                .andExpect(jsonPath("$.items[0].subtotal", is(36000)));
    }

    @Test
    void createOrder_emptyItems_returns400() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "customerName", "Budi",
                "customerEmail", "budi@example.com",
                "items", List.of()
        ));

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors", notNullValue()));
    }

    @Test
    void createOrder_invalidEmail_returns400() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "customerName", "Budi",
                "customerEmail", "bukan-email",
                "items", List.of(Map.of("productId", 1, "quantity", 1))
        ));

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors", notNullValue()));
    }

    @Test
    void createOrder_inactiveProduct_returns400() throws Exception {
        activeProduct.setStatus("INACTIVE");
        when(catalogClient.getProduct(anyLong())).thenReturn(activeProduct);

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(orderJson("Siti", "siti@example.com", 1L, 1)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createOrder_insufficientStock_returns400() throws Exception {
        activeProduct.setStock(1);
        when(catalogClient.getProduct(anyLong())).thenReturn(activeProduct);

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(orderJson("Siti", "siti@example.com", 1L, 10)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createOrder_productNotFound_returns400() throws Exception {
        when(catalogClient.getProduct(anyLong()))
                .thenThrow(new CatalogServiceException("Product not found with id: 999"));

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(orderJson("Siti", "siti@example.com", 999L, 1)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getAllOrders_returns200WithPaginationStructure() throws Exception {
        createOrder();

        mockMvc.perform(get("/api/orders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", notNullValue()))
                .andExpect(jsonPath("$.page", is(0)))
                .andExpect(jsonPath("$.totalElements", notNullValue()));
    }

    @Test
    void getOrderById_existingOrder_returns200() throws Exception {
        long id = createOrder();

        mockMvc.perform(get("/api/orders/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is((int) id)))
                .andExpect(jsonPath("$.customerName", is("Siti")))
                .andExpect(jsonPath("$.status", is("PENDING")));
    }

    @Test
    void getOrderById_notFound_returns404() throws Exception {
        mockMvc.perform(get("/api/orders/9999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)));
    }

    @Test
    void payOrder_pendingOrder_returns200WithPaidStatus() throws Exception {
        long id = createOrder();

        mockMvc.perform(patch("/api/orders/{id}/pay", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("PAID")));
    }

    @Test
    void payOrder_alreadyPaid_returns409() throws Exception {
        long id = createOrder();
        mockMvc.perform(patch("/api/orders/{id}/pay", id));

        mockMvc.perform(patch("/api/orders/{id}/pay", id))
                .andExpect(status().isConflict());
    }

    @Test
    void cancelOrder_pendingOrder_returns200WithCancelledStatus() throws Exception {
        long id = createOrder();

        mockMvc.perform(patch("/api/orders/{id}/cancel", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("CANCELLED")));
    }

    @Test
    void cancelOrder_paidOrder_returns409() throws Exception {
        long id = createOrder();
        mockMvc.perform(patch("/api/orders/{id}/pay", id));

        mockMvc.perform(patch("/api/orders/{id}/cancel", id))
                .andExpect(status().isConflict());
    }
}
