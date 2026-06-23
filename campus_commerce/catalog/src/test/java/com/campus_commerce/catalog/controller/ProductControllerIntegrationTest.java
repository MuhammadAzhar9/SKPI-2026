package com.campus_commerce.catalog.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:catalogtestdb;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.show-sql=false"
})
class ProductControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String productJson(String sku, String name, int price, int stock) throws Exception {
        return objectMapper.writeValueAsString(Map.of(
                "sku", sku, "name", name, "price", price, "stock", stock));
    }

    private long createProduct(String sku) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(productJson(sku, "Produk " + sku, 10000, 10)))
                .andExpect(status().isCreated())
                .andReturn();
        return com.jayway.jsonpath.JsonPath.parse(result.getResponse().getContentAsString())
                .read("$.id", Long.class);
    }

    @Test
    void createProduct_validRequest_returns201WithActiveStatus() throws Exception {
        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(productJson("SKU-INT-001", "Mie Goreng", 5000, 20)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.sku", is("SKU-INT-001")))
                .andExpect(jsonPath("$.name", is("Mie Goreng")))
                .andExpect(jsonPath("$.price", is(5000)))
                .andExpect(jsonPath("$.stock", is(20)))
                .andExpect(jsonPath("$.status", is("ACTIVE")))
                .andExpect(jsonPath("$.id", notNullValue()));
    }

    @Test
    void createProduct_duplicateSku_returns409() throws Exception {
        createProduct("SKU-DUP");

        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(productJson("SKU-DUP", "Produk Lain", 8000, 5)))
                .andExpect(status().isConflict());
    }

    @Test
    void createProduct_blankName_returns400WithFieldError() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "sku", "SKU-X", "name", "", "price", 5000, "stock", 10));

        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors", hasKey("name")));
    }

    @Test
    void createProduct_zeroPriceInvalid_returns400() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "sku", "SKU-Z", "name", "Produk", "price", 0, "stock", 10));

        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors", hasKey("price")));
    }

    @Test
    void getAllProducts_returns200WithPaginationStructure() throws Exception {
        createProduct("SKU-A");
        createProduct("SKU-B");

        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", notNullValue()))
                .andExpect(jsonPath("$.page", is(0)))
                .andExpect(jsonPath("$.totalElements", notNullValue()))
                .andExpect(jsonPath("$.totalPages", notNullValue()));
    }

    @Test
    void getProductById_existingProduct_returns200() throws Exception {
        long id = createProduct("SKU-GET");

        mockMvc.perform(get("/api/products/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is((int) id)))
                .andExpect(jsonPath("$.sku", is("SKU-GET")));
    }

    @Test
    void getProductById_notFound_returns404() throws Exception {
        mockMvc.perform(get("/api/products/9999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)));
    }

    @Test
    void reduceStock_validQuantity_stockDecreases() throws Exception {
        long id = createProduct("SKU-STOCK");

        mockMvc.perform(patch("/api/products/{id}/reduce-stock", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("quantity", 3))))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/products/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stock", is(7)));
    }

    @Test
    void reduceStock_insufficientStock_returns400() throws Exception {
        long id = createProduct("SKU-LOW");

        mockMvc.perform(patch("/api/products/{id}/reduce-stock", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("quantity", 999))))
                .andExpect(status().isBadRequest());
    }
}
