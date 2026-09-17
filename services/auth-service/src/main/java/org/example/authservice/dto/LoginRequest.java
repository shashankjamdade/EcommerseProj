package org.example.authservice.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.example.authservice.model.Role;

public record LoginRequest(
        @NotBlank @Email String email,
        @NotBlank String password,
        @NotNull Role role
) {
}

