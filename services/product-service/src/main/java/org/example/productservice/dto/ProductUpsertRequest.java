package org.example.productservice.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.UUID;

public record ProductUpsertRequest(
        @NotBlank @Size(max = 200) String name,
        @NotNull UUID categoryId,
        @NotNull @DecimalMin(value = "0.0", inclusive = false) BigDecimal price,
        @DecimalMin(value = "0.0", inclusive = false) BigDecimal oldPrice,
        @DecimalMin(value = "0.0") BigDecimal rating,
        @Size(max = 10000) String description,
        @NotNull UUID sellerUserId,
        Boolean active
) {
}

