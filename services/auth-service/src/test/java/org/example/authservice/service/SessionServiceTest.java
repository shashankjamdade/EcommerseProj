package org.example.authservice.service;

import org.example.authservice.model.AuthSession;
import org.example.authservice.model.Role;
import org.example.authservice.repository.AuthSessionRepository;
import org.example.authservice.security.JwtProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SessionServiceTest {

    @Mock
    private AuthSessionRepository authSessionRepository;

    @Captor
    private ArgumentCaptor<AuthSession> sessionCaptor;

    private SessionService sessionService;

    @BeforeEach
    void setUp() {
        JwtProperties jwtProperties = new JwtProperties();
        jwtProperties.setExpiration(Duration.ofMinutes(15));
        Clock fixedClock = Clock.fixed(Instant.parse("2026-09-18T12:00:00Z"), ZoneOffset.UTC);
        sessionService = new SessionService(authSessionRepository, fixedClock, jwtProperties);
    }

    @Test
    void shouldCreateActiveSessionWithInitialExpiry() {
        when(authSessionRepository.save(any(AuthSession.class)))
                .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        UUID userId = UUID.fromString("11111111-2222-3333-4444-555555555555");

        StepVerifier.create(sessionService.createSession(userId, Role.SHOPKEEPER))
                .assertNext(session -> {
                    assertThat(session.getUserId()).isEqualTo(userId);
                    assertThat(session.getActiveRole()).isEqualTo(Role.SHOPKEEPER);
                    assertThat(session.getLoginTime()).isEqualTo(Instant.parse("2026-09-18T12:00:00Z"));
                    assertThat(session.getLastAccessTime()).isEqualTo(Instant.parse("2026-09-18T12:00:00Z"));
                    assertThat(session.getExpiresAt()).isEqualTo(Instant.parse("2026-09-18T12:15:00Z"));
                    assertThat(session.isExpired()).isFalse();
                })
                .verifyComplete();

        verify(authSessionRepository).save(sessionCaptor.capture());
        assertThat(sessionCaptor.getValue().getId()).isNotNull();
    }

    @Test
    void shouldRejectExpiredSessionDuringValidation() {
        UUID sessionId = UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee");
        AuthSession expiredSession = new AuthSession(
                sessionId,
                UUID.fromString("12345678-1234-1234-1234-123456789012"),
                Role.USER,
                Instant.parse("2026-09-18T11:00:00Z"),
                Instant.parse("2026-09-18T11:45:00Z"),
                Instant.parse("2026-09-18T11:59:00Z"),
                null,
                false
        );

        when(authSessionRepository.findById(sessionId)).thenReturn(Mono.just(expiredSession));
        when(authSessionRepository.save(any(AuthSession.class))).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        StepVerifier.create(sessionService.validateActiveSession(sessionId, expiredSession.getUserId(), Role.USER))
                .expectErrorSatisfies(throwable -> {
                    assertThat(throwable).isInstanceOf(ResponseStatusException.class);
                    ResponseStatusException exception = (ResponseStatusException) throwable;
                    assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
                })
                .verify();

        verify(authSessionRepository).save(sessionCaptor.capture());
        assertThat(sessionCaptor.getValue().isExpired()).isTrue();
        assertThat(sessionCaptor.getValue().getLogoutTime()).isEqualTo(Instant.parse("2026-09-18T12:00:00Z"));
    }

    @Test
    void shouldExtendSessionWhenRequestIsAuthenticated() {
        UUID sessionId = UUID.fromString("99999999-8888-7777-6666-555555555555");
        AuthSession activeSession = new AuthSession(
                sessionId,
                UUID.fromString("abcdefab-cdef-cdef-cdef-abcdefabcdef"),
                Role.ADMIN,
                Instant.parse("2026-09-18T11:30:00Z"),
                Instant.parse("2026-09-18T11:50:00Z"),
                Instant.parse("2026-09-18T12:05:00Z"),
                null,
                false
        );

        when(authSessionRepository.findById(sessionId)).thenReturn(Mono.just(activeSession));
        when(authSessionRepository.save(any(AuthSession.class))).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        StepVerifier.create(sessionService.extendSessionIfActive(sessionId))
                .verifyComplete();

        verify(authSessionRepository).save(sessionCaptor.capture());
        AuthSession updatedSession = sessionCaptor.getValue();
        assertThat(updatedSession.getLastAccessTime()).isEqualTo(Instant.parse("2026-09-18T12:00:00Z"));
        assertThat(updatedSession.getExpiresAt()).isEqualTo(Instant.parse("2026-09-18T12:15:00Z"));
        assertThat(updatedSession.isExpired()).isFalse();
    }
}

