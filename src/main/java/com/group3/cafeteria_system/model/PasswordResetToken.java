package com.group3.cafeteria_system.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "password_reset_tokens")
public class PasswordResetToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String token;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    // Token valid for 30 minutes
    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(nullable = false)
    private Boolean used = false;

    // ── Constructors ──────────────────────────
    public PasswordResetToken() {}

    public PasswordResetToken(String token, Long userId) {
        this.token     = token;
        this.userId    = userId;
        this.expiresAt = LocalDateTime.now().plusMinutes(30);
        this.used      = false;
    }

    // ── Business logic ────────────────────────
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }

    public boolean isValid() {
        return !used && !isExpired();
    }

    // ── Getters and Setters ───────────────────
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime e) { this.expiresAt = e; }

    public Boolean getUsed() { return used; }
    public void setUsed(Boolean used) { this.used = used; }
}