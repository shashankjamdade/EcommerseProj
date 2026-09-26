package org.example.productservice.security;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Component
public class AuthServiceAuthenticationManager implements ReactiveAuthenticationManager {

    private final WebClient authWebClient;

    public AuthServiceAuthenticationManager(WebClient authWebClient) {
        this.authWebClient = authWebClient;
    }

    @Override
    public Mono<Authentication> authenticate(Authentication authentication) {
        return Mono.justOrEmpty(authentication)
                .map(Authentication::getCredentials)
                .map(String::valueOf)
                .filter(StringUtils::hasText)
                .switchIfEmpty(Mono.error(new BadCredentialsException("Missing bearer token")))
                .flatMap(token -> authWebClient.get()
                        .uri("/api/v1/auth/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .retrieve()
                        .onStatus(HttpStatusCode::isError, response -> Mono.error(new BadCredentialsException("Invalid or expired authentication token")))
                        .bodyToMono(AuthUserProfile.class)
                        .map(profile -> (Authentication) new UsernamePasswordAuthenticationToken(
                                new AuthenticatedUser(
                                        UUID.fromString(profile.id()),
                                        profile.email(),
                                        profile.assignedRoles(),
                                        profile.activeRole()
                                ),
                                token,
                                new AuthenticatedUser(
                                        UUID.fromString(profile.id()),
                                        profile.email(),
                                        profile.assignedRoles(),
                                        profile.activeRole()
                                ).authorities()
                        )))
                .onErrorMap(BadCredentialsException.class, exception -> exception)
                .onErrorMap(Exception.class, exception -> new BadCredentialsException("Unable to validate authentication token", exception));
    }
}

