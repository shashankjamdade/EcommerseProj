package org.example.productservice.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ReviewCreateRequest(
        @NotNull @Min(1) @Max(5) Integer rating,
        @Size(max = 200) String title,
        @Size(max = 4000) String comment
) {
}

