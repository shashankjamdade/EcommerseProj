package org.example.productservice.security;

import org.example.productservice.model.Role;

import java.util.Set;

public record AuthUserProfile(
        String id,
        String email,
        Set<Role> assignedRoles,
        Role activeRole
) {
}

