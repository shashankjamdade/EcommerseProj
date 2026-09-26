package org.example.productservice.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ProductImageUpsertRequest(
		@NotBlank @Size(max = 2000) String imageUrl,
		@Size(max = 255) String altText,
		Boolean primary,
		@NotNull @Min(0) Integer displayOrder
) {
}

