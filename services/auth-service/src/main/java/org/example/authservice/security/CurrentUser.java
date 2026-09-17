package org.example.authservice.security;

import org.example.authservice.model.Role;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.security.Principal;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public record CurrentUser(
        UUID userId,
        UUID sessionId,
        String email,
        Set<Role> assignedRoles,
        Role activeRole
) implements Principal {

    public CurrentUser {
        Objects.requireNonNull(userId, "userId is required");
        Objects.requireNonNull(sessionId, "sessionId is required");
        Objects.requireNonNull(email, "email is required");
        Objects.requireNonNull(activeRole, "activeRole is required");
        assignedRoles = assignedRoles == null || assignedRoles.isEmpty()
                ? Set.of(activeRole)
                : Set.copyOf(assignedRoles);
    }

    public Collection<? extends GrantedAuthority> authorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + activeRole.name()));
    }

    @Override
    public String getName() {
        return email;
    }
}

