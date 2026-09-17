package org.example.authservice.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Table("auth_sessions")
public class AuthSession {

    @Id
    private UUID id;

    @Column("user_id")
    private UUID userId;

    @Column("active_role")
    private String activeRoleValue;

    @Column("login_time")
    private Instant loginTime;

    @Column("last_access_time")
    private Instant lastAccessTime;

    @Column("expires_at")
    private Instant expiresAt;

    @Column("logout_time")
    private Instant logoutTime;

    @Column("is_expired")
    private boolean expired;

    public AuthSession() {
    }

    public AuthSession(UUID id,
                       UUID userId,
                       Role activeRole,
                       Instant loginTime,
                       Instant lastAccessTime,
                       Instant expiresAt,
                       Instant logoutTime,
                       boolean expired) {
        this.id = id;
        this.userId = userId;
        setActiveRole(activeRole);
        this.loginTime = loginTime;
        this.lastAccessTime = lastAccessTime;
        this.expiresAt = expiresAt;
        this.logoutTime = logoutTime;
        this.expired = expired;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public Role getActiveRole() {
        return activeRoleValue == null ? null : Role.valueOf(activeRoleValue);
    }

    public void setActiveRole(Role activeRole) {
        this.activeRoleValue = activeRole == null ? null : activeRole.name();
    }

    public String getActiveRoleValue() {
        return activeRoleValue;
    }

    public void setActiveRoleValue(String activeRoleValue) {
        this.activeRoleValue = activeRoleValue;
    }

    public Instant getLoginTime() {
        return loginTime;
    }

    public void setLoginTime(Instant loginTime) {
        this.loginTime = loginTime;
    }

    public Instant getLastAccessTime() {
        return lastAccessTime;
    }

    public void setLastAccessTime(Instant lastAccessTime) {
        this.lastAccessTime = lastAccessTime;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }

    public Instant getLogoutTime() {
        return logoutTime;
    }

    public void setLogoutTime(Instant logoutTime) {
        this.logoutTime = logoutTime;
    }

    public boolean isExpired() {
        return expired;
    }

    public void setExpired(boolean expired) {
        this.expired = expired;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof AuthSession that)) {
            return false;
        }
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}

