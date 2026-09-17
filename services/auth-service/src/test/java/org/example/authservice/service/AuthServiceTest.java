package org.example.authservice.service;

import org.example.authservice.dto.AuthResponse;
import org.example.authservice.dto.LoginRequest;
import org.example.authservice.dto.RegisterRequest;
import org.example.authservice.model.AuthSession;
import org.example.authservice.model.Role;
import org.example.authservice.model.UserAccount;
import org.example.authservice.repository.UserAccountRepository;
import org.example.authservice.security.AuthToken;
import org.example.authservice.security.CurrentUser;
import org.example.authservice.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserAccountRepository userAccountRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private SessionService sessionService;

    @Captor
    private ArgumentCaptor<UserAccount> userAccountCaptor;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        Clock fixedClock = Clock.fixed(Instant.parse("2026-09-18T11:00:00Z"), ZoneOffset.UTC);
        authService = new AuthService(userAccountRepository, passwordEncoder, jwtService, sessionService, fixedClock);
    }

    @Test
    void shouldRegisterUserWithDefaultUserRole() {
        RegisterRequest request = new RegisterRequest("USER@Example.com", "Password123", null);
        UUID userId = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
        UserAccount savedUser = new UserAccount(
                userId,
                "user@example.com",
                "hashed-password",
                Set.of(Role.USER),
                true,
                Instant.parse("2026-09-18T11:00:00Z"),
                Instant.parse("2026-09-18T11:00:00Z")
        );

        when(userAccountRepository.findByEmail("user@example.com")).thenReturn(Mono.empty());
        when(passwordEncoder.encode("Password123")).thenReturn("hashed-password");
        when(userAccountRepository.save(any(UserAccount.class))).thenReturn(Mono.just(savedUser));

        StepVerifier.create(authService.register(request))
                .assertNext(response -> {
                    assertThat(response.id()).isEqualTo(userId.toString());
                    assertThat(response.email()).isEqualTo("user@example.com");
                    assertThat(response.roles()).containsExactly(Role.USER);
                    assertThat(response.message()).isEqualTo("User registered successfully");
                })
                .verifyComplete();

        verify(userAccountRepository).save(userAccountCaptor.capture());
        UserAccount persistedUser = userAccountCaptor.getValue();
        assertThat(persistedUser.getEmail()).isEqualTo("user@example.com");
        assertThat(persistedUser.getPasswordHash()).isEqualTo("hashed-password");
        assertThat(persistedUser.getRoles()).containsExactly(Role.USER);
    }

    @Test
    void shouldRejectDuplicateEmailDuringRegistration() {
        RegisterRequest request = new RegisterRequest("user@example.com", "Password123", Set.of(Role.USER));
        when(userAccountRepository.findByEmail("user@example.com"))
                .thenReturn(Mono.just(new UserAccount()));

        StepVerifier.create(authService.register(request))
                .expectErrorSatisfies(throwable -> {
                    assertThat(throwable).isInstanceOf(ResponseStatusException.class);
                    ResponseStatusException exception = (ResponseStatusException) throwable;
                    assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
                })
                .verify();
    }

    @Test
    void shouldLoginAndReturnJwtForSelectedRole() {
        UUID userId = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
        UUID sessionId = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");
        UserAccount userAccount = new UserAccount(
                userId,
                "admin@example.com",
                "stored-hash",
                Set.of(Role.ADMIN, Role.USER),
                true,
                Instant.now(),
                Instant.now()
        );
        AuthSession authSession = new AuthSession(
                sessionId,
                userId,
                Role.ADMIN,
                Instant.parse("2026-09-18T11:00:00Z"),
                Instant.parse("2026-09-18T11:00:00Z"),
                Instant.parse("2026-09-18T11:15:00Z"),
                null,
                false
        );
        AuthToken authToken = new AuthToken("jwt-token", Instant.parse("2026-09-18T11:15:00Z"));

        when(userAccountRepository.findByEmail("admin@example.com")).thenReturn(Mono.just(userAccount));
        when(passwordEncoder.matches("Password123", "stored-hash")).thenReturn(true);
        when(sessionService.createSession(userId, Role.ADMIN)).thenReturn(Mono.just(authSession));
        when(jwtService.issueToken(userAccount, Role.ADMIN, sessionId, authSession.getExpiresAt())).thenReturn(authToken);

        StepVerifier.create(authService.login(new LoginRequest("admin@example.com", "Password123", Role.ADMIN)))
                .assertNext(response -> assertSuccessfulLogin(response, userAccount, authToken))
                .verifyComplete();
    }

    @Test
    void shouldRejectLoginWhenRoleIsNotAssigned() {
        UserAccount userAccount = new UserAccount(
                UUID.fromString("dddddddd-dddd-dddd-dddd-dddddddddddd"),
                "user@example.com",
                "stored-hash",
                Set.of(Role.USER),
                true,
                Instant.now(),
                Instant.now()
        );

        when(userAccountRepository.findByEmail("user@example.com")).thenReturn(Mono.just(userAccount));
        when(passwordEncoder.matches(anyString(), eq("stored-hash"))).thenReturn(true);

        StepVerifier.create(authService.login(new LoginRequest("user@example.com", "Password123", Role.ADMIN)))
                .expectErrorSatisfies(throwable -> {
                    assertThat(throwable).isInstanceOf(ResponseStatusException.class);
                    ResponseStatusException exception = (ResponseStatusException) throwable;
                    assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
                })
                .verify();
    }

    @Test
    void shouldLogoutAndMarkSessionExpired() {
        UUID userId = UUID.fromString("eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee");
        UUID sessionId = UUID.fromString("ffffffff-ffff-ffff-ffff-ffffffffffff");
        CurrentUser currentUser = new CurrentUser(userId, sessionId, "user@example.com", Set.of(Role.USER), Role.USER);
        AuthSession expiredSession = new AuthSession(
                sessionId,
                userId,
                Role.USER,
                Instant.parse("2026-09-18T11:00:00Z"),
                Instant.parse("2026-09-18T11:12:00Z"),
                Instant.parse("2026-09-18T11:12:00Z"),
                Instant.parse("2026-09-18T11:12:00Z"),
                true
        );

        when(sessionService.logout(sessionId)).thenReturn(Mono.just(expiredSession));

        StepVerifier.create(authService.logout(currentUser))
                .assertNext(response -> {
                    assertThat(response.message()).isEqualTo("Logged out successfully");
                    assertThat(response.logoutTime()).isEqualTo(Instant.parse("2026-09-18T11:12:00Z"));
                    assertThat(response.isExpired()).isTrue();
                })
                .verifyComplete();
    }

    private void assertSuccessfulLogin(AuthResponse response, UserAccount userAccount, AuthToken authToken) {
        assertThat(response.accessToken()).isEqualTo("jwt-token");
        assertThat(response.tokenType()).isEqualTo("Bearer");
        assertThat(response.sessionId()).isEqualTo(UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc"));
        assertThat(response.loginTime()).isEqualTo(Instant.parse("2026-09-18T11:00:00Z"));
        assertThat(response.expiresAt()).isEqualTo(authToken.expiresAt());
        assertThat(response.isExpired()).isFalse();
        assertThat(response.email()).isEqualTo(userAccount.getEmail());
        assertThat(response.assignedRoles()).containsExactlyInAnyOrder(Role.ADMIN, Role.USER);
        assertThat(response.activeRole()).isEqualTo(Role.ADMIN);
    }
}

