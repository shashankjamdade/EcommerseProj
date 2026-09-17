package org.example.authservice.security;

import io.jsonwebtoken.JwtException;
import org.example.authservice.model.Role;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    @Test
    void shouldIssueAndParseToken() {
        JwtProperties properties = new JwtProperties();
        properties.setSecret("01234567890123456789012345678901");
        properties.setIssuer("auth-service-test");
        properties.setExpiration(Duration.ofMinutes(15));
        Clock fixedClock = Clock.fixed(Instant.parse("2026-09-18T10:15:30Z"), ZoneOffset.UTC);

        JwtService jwtService = new JwtService(properties, fixedClock);

        UUID userId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        UUID sessionId = UUID.fromString("22222222-2222-2222-2222-222222222222");

        AuthToken authToken = jwtService.issueToken(
                userId,
                sessionId,
                "admin@example.com",
                Set.of(Role.ADMIN, Role.USER),
                Role.ADMIN,
                Instant.parse("2026-09-18T10:30:30Z")
        );

        CurrentUser currentUser = jwtService.parseToken(authToken.token());

        assertThat(authToken.expiresAt()).isEqualTo(Instant.parse("2026-09-18T10:30:30Z"));
        assertThat(currentUser.userId()).isEqualTo(userId);
        assertThat(currentUser.sessionId()).isEqualTo(sessionId);
        assertThat(currentUser.email()).isEqualTo("admin@example.com");
        assertThat(currentUser.activeRole()).isEqualTo(Role.ADMIN);
        assertThat(currentUser.assignedRoles()).containsExactlyInAnyOrder(Role.ADMIN, Role.USER);
        assertThat(currentUser.authorities())
                .extracting(Object::toString)
                .containsExactly("ROLE_ADMIN");
    }

    @Test
    void shouldRejectMalformedToken() {
        JwtProperties properties = new JwtProperties();
        properties.setSecret("01234567890123456789012345678901");

        JwtService jwtService = new JwtService(properties, Clock.systemUTC());

        assertThatThrownBy(() -> jwtService.parseToken("not-a-real-token"))
                .isInstanceOf(JwtException.class);
    }
}

