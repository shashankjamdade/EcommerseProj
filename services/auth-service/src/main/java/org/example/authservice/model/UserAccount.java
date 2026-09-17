package org.example.authservice.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Table("users")
public class UserAccount {

    @Id
    private UUID id;

    private String email;

    @Column("password_hash")
    private String passwordHash;

    @Column("roles")
    private String rolesCsv;

    private boolean active = true;

    @Column("created_at")
    private Instant createdAt;

    @Column("updated_at")
    private Instant updatedAt;

    public UserAccount() {
    }

    public UserAccount(UUID id,
                       String email,
                       String passwordHash,
                       Set<Role> roles,
                       boolean active,
                       Instant createdAt,
                       Instant updatedAt) {
        this.id = id;
        this.email = email;
        this.passwordHash = passwordHash;
        setRoles(roles);
        this.active = active;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public Set<Role> getRoles() {
        if (rolesCsv == null || rolesCsv.isBlank()) {
            return Set.of();
        }
        return Set.copyOf(java.util.Arrays.stream(rolesCsv.split(","))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .map(Role::valueOf)
                .collect(Collectors.toCollection(LinkedHashSet::new)));
    }

    public void setRoles(Set<Role> roles) {
        Set<Role> normalizedRoles = roles == null ? new HashSet<>() : new LinkedHashSet<>(roles);
        this.rolesCsv = normalizedRoles.stream()
                .map(Enum::name)
                .sorted()
                .collect(Collectors.joining(","));
    }

    public String getRolesCsv() {
        return rolesCsv;
    }

    public void setRolesCsv(String rolesCsv) {
        this.rolesCsv = rolesCsv;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof UserAccount that)) {
            return false;
        }
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}

