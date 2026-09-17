package org.example.authservice.dto;

import org.example.authservice.model.Role;

import java.time.Instant;
import java.util.Set;

public record AdminUserDetailsResponse(
        String id,
        String email,
        Set<Role> roles,
        boolean active,
        Instant createdAt,
        Instant updatedAt
) {
}

