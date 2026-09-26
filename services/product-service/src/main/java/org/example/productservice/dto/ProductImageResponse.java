package org.example.productservice.dto;

import java.time.Instant;

public record ProductImageResponse(
        String id,
        String productId,
        String imageUrl,
        String altText,
        boolean primary,
        Integer displayOrder,
        Instant createdAt,
        Instant updatedAt
) {
}
