package org.example.productservice.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record ProductDetailsResponse(
        String id,
        String name,
        CategoryResponse category,
        BigDecimal price,
        BigDecimal oldPrice,
        BigDecimal rating,
        long reviewCount,
        String description,
        SellerResponse seller,
        boolean active,
        List<VariantResponse> variants,
        List<SpecificationResponse> specifications,
        List<ProductImageResponse> images,
        Instant createdAt,
        Instant updatedAt
) {
}

