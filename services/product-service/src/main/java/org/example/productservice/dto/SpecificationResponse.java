package org.example.productservice.dto;

public record SpecificationResponse(
        String id,
        String productId,
        String label,
        String value,
        String unit,
        Integer displayOrder
) {
}

