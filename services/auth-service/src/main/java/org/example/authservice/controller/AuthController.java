package org.example.authservice.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.example.authservice.dto.AdminUserDetailsResponse;
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
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;
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
    public Mono<UserProfileResponse> me(Authentication authentication) {
        return authService.getProfile(currentUser(authentication));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/users/by-email")
    public Mono<AdminUserDetailsResponse> getUserByEmail(
            Authentication authentication,
            @RequestParam("email") @NotBlank @Email String email) {
        return authService.getUserByEmail(email);
    }

    @PostMapping("/logout")
    public Mono<ResponseEntity<LogoutResponse>> logout(Authentication authentication) {
        return authService.logout(currentUser(authentication))
                .map(ResponseEntity::ok);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin/ping")
    public Mono<Map<String, String>> adminPing(Authentication authentication) {
        CurrentUser currentUser = currentUser(authentication);
        return Mono.just(Map.of(
                "message", "Admin access granted",
                "email", currentUser.email(),
                "activeRole", currentUser.activeRole().name()
        ));
    }

    private CurrentUser currentUser(Authentication authentication) {
        return (CurrentUser) authentication.getPrincipal();
    }
}

