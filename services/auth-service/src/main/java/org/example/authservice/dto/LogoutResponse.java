package org.example.authservice.dto;

import java.time.Instant;

public record LogoutResponse(
        String message,
        Instant logoutTime,
        boolean isExpired
) {
}

