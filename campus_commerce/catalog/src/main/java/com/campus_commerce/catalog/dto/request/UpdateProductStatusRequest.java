package com.campus_commerce.catalog.dto.request;

import com.campus_commerce.catalog.model.enums.ProductStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateProductStatusRequest {

    @NotNull(message = "Status is required")
    private ProductStatus status;
}
