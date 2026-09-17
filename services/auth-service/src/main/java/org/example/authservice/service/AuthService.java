package org.example.authservice.service;

import org.example.authservice.dto.AuthResponse;
import org.example.authservice.dto.AdminUserDetailsResponse;
import org.example.authservice.dto.LoginRequest;
import org.example.authservice.dto.LogoutResponse;
import org.example.authservice.dto.RegisterRequest;
import org.example.authservice.dto.RegisterResponse;
import org.example.authservice.dto.UserProfileResponse;
import org.example.authservice.model.AuthSession;
import org.example.authservice.model.Role;
import org.example.authservice.model.UserAccount;
import org.example.authservice.repository.UserAccountRepository;
import org.example.authservice.security.AuthToken;
import org.example.authservice.security.CurrentUser;
import org.example.authservice.security.JwtService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

import java.time.Clock;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
public class AuthService {

    private final UserAccountRepository userAccountRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final SessionService sessionService;
    private final Clock clock;

    public AuthService(UserAccountRepository userAccountRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       SessionService sessionService,
                       Clock clock) {
        this.userAccountRepository = userAccountRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.sessionService = sessionService;
        this.clock = clock;
    }

    public Mono<RegisterResponse> register(RegisterRequest request) {
        String normalizedEmail = normalizeEmail(request.email());
        Set<Role> requestedRoles = normalizeRoles(request.roles());
        Instant now = Instant.now(clock);

        return userAccountRepository.findByEmail(normalizedEmail)
                .hasElement()
                .flatMap(exists -> exists
                        ? Mono.error(new ResponseStatusException(HttpStatus.CONFLICT, "User already registered with this email"))
                        : userAccountRepository.save(new UserAccount(
                                UUID.randomUUID(),
                                normalizedEmail,
                                passwordEncoder.encode(request.password()),
                                requestedRoles,
                                true,
                                now,
                                now
                        )))
                .map(savedUser -> new RegisterResponse(
                        savedUser.getId().toString(),
                        savedUser.getEmail(),
                        savedUser.getRoles(),
                        savedUser.getCreatedAt(),
                        "User registered successfully"
                ))
                .onErrorMap(DataIntegrityViolationException.class,
                        exception -> new ResponseStatusException(HttpStatus.CONFLICT,
                                "User already registered with this email", exception));
    }

    public Mono<AuthResponse> login(LoginRequest request) {
        String normalizedEmail = normalizeEmail(request.email());
        Role selectedRole = request.role();

        return userAccountRepository.findByEmail(normalizedEmail)
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email, password, or role")))
                .flatMap(userAccount -> {
                    if (!userAccount.isActive()) {
                        return Mono.error(new ResponseStatusException(HttpStatus.FORBIDDEN, "User account is disabled"));
                    }
                    if (!passwordEncoder.matches(request.password(), userAccount.getPasswordHash())) {
                        return Mono.error(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email, password, or role"));
                    }
                    if (!userAccount.getRoles().contains(selectedRole)) {
                        return Mono.error(new ResponseStatusException(HttpStatus.FORBIDDEN, "User is not allowed to login with the requested role"));
                    }

                    return sessionService.createSession(userAccount.getId(), selectedRole)
                            .map(session -> toAuthResponse(userAccount, session,
                                    jwtService.issueToken(userAccount, selectedRole, session.getId(), session.getExpiresAt())));
                });
    }

    public Mono<UserProfileResponse> getProfile(CurrentUser currentUser) {
        return userAccountRepository.findById(currentUser.userId())
                .map(userAccount -> new UserProfileResponse(
                        userAccount.getId().toString(),
                        userAccount.getEmail(),
                        userAccount.getRoles(),
                        currentUser.activeRole()
                ))
                .switchIfEmpty(Mono.just(new UserProfileResponse(
                        currentUser.userId().toString(),
                        currentUser.email(),
                        currentUser.assignedRoles(),
                        currentUser.activeRole()
                )));
    }

    public Mono<AdminUserDetailsResponse> getUserByEmail(String email) {
        String normalizedEmail = normalizeEmail(email);

        return userAccountRepository.findByEmail(normalizedEmail)
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found for the provided email")))
                .map(userAccount -> new AdminUserDetailsResponse(
                        userAccount.getId().toString(),
                        userAccount.getEmail(),
                        userAccount.getRoles(),
                        userAccount.isActive(),
                        userAccount.getCreatedAt(),
                        userAccount.getUpdatedAt()
                ));
    }

    public Mono<LogoutResponse> logout(CurrentUser currentUser) {
        return sessionService.logout(currentUser.sessionId())
                .map(session -> new LogoutResponse(
                        "Logged out successfully",
                        session.getLogoutTime(),
                        session.isExpired()
                ));
    }

    private AuthResponse toAuthResponse(UserAccount userAccount, AuthSession authSession, AuthToken authToken) {
        return new AuthResponse(
                authToken.token(),
                "Bearer",
                authSession.getId(),
                authSession.getLoginTime(),
                authToken.expiresAt(),
                authSession.isExpired(),
                userAccount.getEmail(),
                userAccount.getRoles(),
                authSession.getActiveRole()
        );
    }

    private String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }

    private Set<Role> normalizeRoles(Set<Role> roles) {
        if (roles == null || roles.isEmpty()) {
            return Set.of(Role.USER);
        }
        return Set.copyOf(new LinkedHashSet<>(roles));
    }
}

