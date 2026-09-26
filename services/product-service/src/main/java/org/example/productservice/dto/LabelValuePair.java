package org.example.productservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LabelValuePair(
        @NotBlank @Size(max = 120) String label,
        @NotBlank @Size(max = 5000) String value
) {
}

