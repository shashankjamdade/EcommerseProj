package org.example.productservice.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record VariantResponse(
        String id,
        String productId,
        String name,
        String sku,
        BigDecimal price,
        BigDecimal oldPrice,
        Integer stockQuantity,
        List<LabelValuePair> attributes,
        boolean active,
        Instant createdAt,
        Instant updatedAt
) {
}

