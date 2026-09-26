package org.example.productservice.dto;

import java.time.Instant;

public record ReviewResponse(
        String id,
        String productId,
        String reviewerEmail,
        Integer rating,
        String title,
        String comment,
        Instant createdAt,
        Instant updatedAt
) {
}

