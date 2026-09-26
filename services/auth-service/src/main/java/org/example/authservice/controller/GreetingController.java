package org.example.authservice.controller;

import org.springframework.boot.info.BuildProperties;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/auth")
public class GreetingController {

    private final Optional<BuildProperties> buildProperties;

    public GreetingController(Optional<BuildProperties> buildProperties) {
        this.buildProperties = buildProperties;
    }

    @GetMapping("/greeting")
    public Mono<ResponseEntity<Map<String, String>>> greeting() {
        return Mono.fromCallable(() -> {
            Map<String, String> response = Map.ofEntries(
                    Map.entry("service", "auth-service"),
                    Map.entry("message", "Auth Service is running"),
                    Map.entry("servicePath", "/api/v1/auth/greeting"),
                    Map.entry("gatewayPath", "/auth/greeting"),
                    Map.entry("status", "UP"),
                    Map.entry("version", buildProperties.map(BuildProperties::getVersion).orElse("unknown"))
            );
            return ResponseEntity.ok(response);
        });
    }
}

