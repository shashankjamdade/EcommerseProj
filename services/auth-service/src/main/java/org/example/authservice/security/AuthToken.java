package org.example.authservice.security;

import java.time.Instant;

public record AuthToken(String token, Instant expiresAt) {
}

