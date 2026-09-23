package com.travelmanagementsystem.identity.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "email_verification_tokens")
public class EmailVerificationToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "token_hash", nullable = false)
    private String tokenHash;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "used", nullable = false)
    private boolean used;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private VerificationTokenType type;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected EmailVerificationToken() {
    }

    public EmailVerificationToken(User user, String tokenHash, Instant expiresAt, VerificationTokenType type) {
        this.user = user;
        this.tokenHash = tokenHash;
        this.expiresAt = expiresAt;
        this.used = false;
        this.type = type;
        this.createdAt = Instant.now();
    }

    @PrePersist
    void prePersist() {
        this.createdAt = Instant.now();
    }

    public void markUsed() {
        this.used = true;
    }

    public boolean isUsed() {
        return used;
    }

    public boolean isExpired() {
        return Instant.now().isAfter(this.expiresAt);
    }

    public boolean isValid() {
        return !isUsed() && !isExpired();
    }

    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public String getTokenHash() {
        return tokenHash;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public VerificationTokenType getType() {
        return type;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
