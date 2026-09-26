package org.example.productservice.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.validation.Valid;

import java.math.BigDecimal;
import java.util.List;

public record VariantUpsertRequest(
        @NotBlank @Size(max = 200) String name,
        @NotBlank @Size(max = 120) String sku,
        @NotNull @DecimalMin(value = "0.0", inclusive = false) BigDecimal price,
        @DecimalMin(value = "0.0", inclusive = false) BigDecimal oldPrice,
        @NotNull @Min(0) Integer stockQuantity,
        List<@Valid LabelValuePair> attributes,
        Boolean active
) {
}

