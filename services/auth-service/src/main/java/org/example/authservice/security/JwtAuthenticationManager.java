package org.example.authservice.security;

import io.jsonwebtoken.JwtException;
import org.example.authservice.service.SessionService;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Mono;

@Component
public class JwtAuthenticationManager implements ReactiveAuthenticationManager {

    private final JwtService jwtService;
    private final SessionService sessionService;

    public JwtAuthenticationManager(JwtService jwtService, SessionService sessionService) {
        this.jwtService = jwtService;
        this.sessionService = sessionService;
    }

    @Override
    public Mono<Authentication> authenticate(Authentication authentication) {
        return Mono.justOrEmpty(authentication)
                .map(Authentication::getCredentials)
                .map(String::valueOf)
                .filter(StringUtils::hasText)
                .switchIfEmpty(Mono.error(new BadCredentialsException("Missing bearer token")))
                .map(jwtService::parseToken)
                .flatMap(currentUser -> sessionService.validateActiveSession(
                                currentUser.sessionId(),
                                currentUser.userId(),
                                currentUser.activeRole())
                        .thenReturn((Authentication) new UsernamePasswordAuthenticationToken(
                                currentUser,
                                authentication.getCredentials(),
                                currentUser.authorities()
                        )))
                .onErrorMap(JwtException.class,
                        exception -> new BadCredentialsException("Invalid or expired JWT token", exception))
                .onErrorMap(IllegalArgumentException.class,
                        exception -> new BadCredentialsException("Invalid JWT claims", exception))
                .onErrorMap(org.springframework.web.server.ResponseStatusException.class,
                        exception -> new BadCredentialsException(exception.getReason(), exception));
    }
}

