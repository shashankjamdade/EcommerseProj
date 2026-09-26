package org.example.productservice.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SpecificationUpsertRequest(
        @NotBlank @Size(max = 120) String label,
        @NotBlank @Size(max = 5000) String value,
        @Size(max = 60) String unit,
        @NotNull @Min(0) Integer displayOrder
) {
}

