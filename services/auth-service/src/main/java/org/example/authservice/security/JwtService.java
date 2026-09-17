package org.example.authservice.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.example.authservice.model.Role;
import org.example.authservice.model.UserAccount;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Service
public class JwtService {

    private final JwtProperties jwtProperties;
    private final Clock clock;
    private final SecretKey signingKey;

    public JwtService(JwtProperties jwtProperties, Clock clock) {
        this.jwtProperties = jwtProperties;
        this.clock = clock;
        byte[] keyBytes = jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8);
        Assert.isTrue(keyBytes.length >= 32, "JWT secret must be at least 32 bytes long");
        this.signingKey = Keys.hmacShaKeyFor(keyBytes);
    }

    public AuthToken issueToken(UserAccount userAccount, Role activeRole, UUID sessionId, Instant sessionExpiresAt) {
        return issueToken(userAccount.getId(), sessionId, userAccount.getEmail(), userAccount.getRoles(), activeRole, sessionExpiresAt);
    }

    public AuthToken issueToken(UUID userId,
                                UUID sessionId,
                                String email,
                                Set<Role> assignedRoles,
                                Role activeRole,
                                Instant sessionExpiresAt) {
        Objects.requireNonNull(activeRole, "activeRole is required");
        Objects.requireNonNull(userId, "userId is required");
        Objects.requireNonNull(sessionId, "sessionId is required");
        Objects.requireNonNull(sessionExpiresAt, "sessionExpiresAt is required");

        Instant issuedAt = Instant.now(clock);
        Set<Role> effectiveRoles = assignedRoles == null || assignedRoles.isEmpty()
                ? Set.of(activeRole)
                : Set.copyOf(assignedRoles);

        String token = Jwts.builder()
                .subject(userId.toString())
                .id(sessionId.toString())
                .issuer(jwtProperties.getIssuer())
                .issuedAt(Date.from(issuedAt))
                .claim("email", email)
                .claim("assignedRoles", effectiveRoles.stream().map(Enum::name).sorted().toList())
                .claim("activeRole", activeRole.name())
                .signWith(signingKey, Jwts.SIG.HS256)
                .compact();

        return new AuthToken(token, sessionExpiresAt);
    }

    public CurrentUser parseToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(signingKey)
                .requireIssuer(jwtProperties.getIssuer())
                .build()
                .parseSignedClaims(token)
                .getPayload();

        String userId = claims.getSubject();
        String sessionId = claims.getId();
        String email = claims.get("email", String.class);
        String activeRoleValue = claims.get("activeRole", String.class);

        if (userId == null || sessionId == null || email == null || activeRoleValue == null) {
            throw new JwtException("Missing required JWT claims");
        }

        Role activeRole = Role.valueOf(activeRoleValue);
        Set<Role> assignedRoles = new LinkedHashSet<>();
        Object rolesClaim = claims.get("assignedRoles");
        if (rolesClaim instanceof List<?> roles) {
            roles.stream()
                    .map(String::valueOf)
                    .map(Role::valueOf)
                    .forEach(assignedRoles::add);
        }
        assignedRoles.add(activeRole);

        return new CurrentUser(UUID.fromString(userId), UUID.fromString(sessionId), email, assignedRoles, activeRole);
    }
}

