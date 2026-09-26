package org.example.productservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CategoryUpsertRequest(
        @NotBlank @Size(max = 120) String name,
        @Size(max = 2000) String description,
        Boolean active
) {
}

