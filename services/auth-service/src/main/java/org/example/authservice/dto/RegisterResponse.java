package org.example.authservice.dto;

import org.example.authservice.model.Role;

import java.time.Instant;
import java.util.Set;

public record RegisterResponse(
        String id,
        String email,
        Set<Role> roles,
        Instant registeredAt,
        String message
) {
}

