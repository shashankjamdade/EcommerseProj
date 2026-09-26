package org.example.productservice.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record SellerResponse(
        String userId,
        String description,
        String address,
        BigDecimal latitude,
        BigDecimal longitude,
        Integer creditScore,
        Instant createdAt,
        Instant updatedAt
) {
}
