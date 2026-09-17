package org.example.authservice.service;

import org.example.authservice.model.AuthSession;
import org.example.authservice.model.Role;
import org.example.authservice.repository.AuthSessionRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Service
public class SessionService {

    private final AuthSessionRepository authSessionRepository;
    private final Clock clock;
    private final Duration sessionDuration;

    public SessionService(AuthSessionRepository authSessionRepository,
                          Clock clock,
                          org.example.authservice.security.JwtProperties jwtProperties) {
        this.authSessionRepository = authSessionRepository;
        this.clock = clock;
        this.sessionDuration = jwtProperties.getExpiration();
    }

    public Mono<AuthSession> createSession(UUID userId, Role activeRole) {
        Instant now = Instant.now(clock);
        AuthSession authSession = new AuthSession(
                UUID.randomUUID(),
                userId,
                activeRole,
                now,
                now,
                now.plus(sessionDuration),
                null,
                false
        );
        return authSessionRepository.save(authSession);
    }

    public Mono<AuthSession> validateActiveSession(UUID sessionId, UUID userId, Role activeRole) {
        return authSessionRepository.findById(sessionId)
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Session not found")))
                .flatMap(session -> {
                    Instant now = Instant.now(clock);
                    if (session.isExpired()
                            || session.getLogoutTime() != null
                            || session.getExpiresAt() == null
                            || !session.getExpiresAt().isAfter(now)) {
                        return expireSession(session, now)
                                .then(Mono.error(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Session has expired")));
                    }
                    if (!session.getUserId().equals(userId) || session.getActiveRole() != activeRole) {
                        return Mono.error(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Session does not match token claims"));
                    }
                    return Mono.just(session);
                });
    }

    public Mono<Void> extendSessionIfActive(UUID sessionId) {
        return authSessionRepository.findById(sessionId)
                .flatMap(session -> {
                    Instant now = Instant.now(clock);
                    if (session.isExpired() || session.getLogoutTime() != null || session.getExpiresAt() == null) {
                        return Mono.empty();
                    }
                    if (!session.getExpiresAt().isAfter(now)) {
                        return expireSession(session, now).then();
                    }
                    session.setLastAccessTime(now);
                    session.setExpiresAt(now.plus(sessionDuration));
                    return authSessionRepository.save(session).then();
                })
                .then();
    }

    public Mono<AuthSession> logout(UUID sessionId) {
        return authSessionRepository.findById(sessionId)
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "Session not found")))
                .flatMap(session -> {
                    Instant now = Instant.now(clock);
                    session.setExpired(true);
                    session.setLogoutTime(now);
                    session.setExpiresAt(now);
                    session.setLastAccessTime(now);
                    return authSessionRepository.save(session);
                });
    }

    private Mono<AuthSession> expireSession(AuthSession session, Instant now) {
        session.setExpired(true);
        if (session.getLogoutTime() == null) {
            session.setLogoutTime(now);
        }
        if (session.getLastAccessTime() == null) {
            session.setLastAccessTime(now);
        }
        if (session.getExpiresAt() == null || session.getExpiresAt().isAfter(now)) {
            session.setExpiresAt(now);
        }
        return authSessionRepository.save(session);
    }
}

