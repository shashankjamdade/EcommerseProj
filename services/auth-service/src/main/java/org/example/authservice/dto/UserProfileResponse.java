package org.example.authservice.dto;

import org.example.authservice.model.Role;

import java.util.Set;

public record UserProfileResponse(
        String id,
        String email,
        Set<Role> assignedRoles,
        Role activeRole
) {
}

