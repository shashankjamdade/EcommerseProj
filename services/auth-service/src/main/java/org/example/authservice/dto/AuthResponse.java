package org.example.authservice.dto;

import org.example.authservice.model.Role;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public record AuthResponse(
        String accessToken,
        String tokenType,
        UUID sessionId,
        Instant loginTime,
        Instant expiresAt,
        boolean isExpired,
        String email,
        Set<Role> assignedRoles,
        Role activeRole
) {
}

