package org.example.authservice.security;

import org.example.authservice.service.SessionService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

@Component
public class JwtRefreshWebFilter implements WebFilter {

    private final SessionService sessionService;

    public JwtRefreshWebFilter(SessionService sessionService) {
        this.sessionService = sessionService;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        exchange.getResponse().beforeCommit(() -> exchange.getPrincipal()
                .cast(Authentication.class)
                .filter(Authentication::isAuthenticated)
                .map(Authentication::getPrincipal)
                .filter(CurrentUser.class::isInstance)
                .cast(CurrentUser.class)
                .flatMap(currentUser -> sessionService.extendSessionIfActive(currentUser.sessionId()))
                .then());

        return chain.filter(exchange);
    }
}

