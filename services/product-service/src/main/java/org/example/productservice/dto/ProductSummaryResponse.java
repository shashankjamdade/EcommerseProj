package org.example.productservice.dto;

import java.math.BigDecimal;

public record ProductSummaryResponse(
        String id,
        String name,
        String categoryId,
        String categoryName,
        BigDecimal price,
        BigDecimal oldPrice,
        BigDecimal rating,
        SellerResponse seller,
        String description
) {
}

