package org.example.productservice.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.UUID;

public record SellerUpsertRequest(
		@NotNull UUID userId,
		@Size(max = 5000) String description,
		@Size(max = 1000) String address,
		@DecimalMin(value = "-90.0") @DecimalMax(value = "90.0") BigDecimal latitude,
		@DecimalMin(value = "-180.0") @DecimalMax(value = "180.0") BigDecimal longitude,
		@Max(1000) Integer creditScore
) {
}

