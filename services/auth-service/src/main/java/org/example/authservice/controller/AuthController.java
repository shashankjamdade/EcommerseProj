package org.example.authservice.controller;

import jakarta.validation.Valid;
import org.example.authservice.dto.AuthResponse;
import org.example.authservice.dto.LoginRequest;
import org.example.authservice.dto.LogoutResponse;
import org.example.authservice.dto.RegisterRequest;
import org.example.authservice.dto.RegisterResponse;
import org.example.authservice.dto.UserProfileResponse;
import org.example.authservice.security.CurrentUser;
import org.example.authservice.service.AuthService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.Map;

@RestController
@Validated
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public Mono<ResponseEntity<RegisterResponse>> register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request)
                .map(response -> ResponseEntity.status(HttpStatus.CREATED).body(response));
    }

    @PostMapping("/login")
    public Mono<ResponseEntity<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request)
                .map(ResponseEntity::ok);
    }

    @GetMapping("/me")
    public Mono<UserProfileResponse> me(@AuthenticationPrincipal CurrentUser currentUser) {
        return authService.getProfile(currentUser);
    }

    @PostMapping("/logout")
    public Mono<ResponseEntity<LogoutResponse>> logout(@AuthenticationPrincipal CurrentUser currentUser) {
        return authService.logout(currentUser)
                .map(ResponseEntity::ok);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin/ping")
    public Mono<Map<String, String>> adminPing(@AuthenticationPrincipal CurrentUser currentUser) {
        return Mono.just(Map.of(
                "message", "Admin access granted",
                "email", currentUser.email(),
                "activeRole", currentUser.activeRole().name()
        ));
    }
}

