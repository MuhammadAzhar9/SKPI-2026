package com.campus_commerce.catalog.dto.request;

import com.campus_commerce.catalog.model.enums.StockOperation;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateStockRequest {

    @NotNull(message = "Quantity is required")
    @Positive(message = "Quantity must be greater than 0")
    private Integer quantity;

    @NotNull(message = "Operation is required")
    private StockOperation operation;
}
