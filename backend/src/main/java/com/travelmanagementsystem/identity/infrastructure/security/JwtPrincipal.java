package com.travelmanagementsystem.identity.infrastructure.security;

import java.util.Objects;

public class JwtPrincipal {

    private final Long userId;
    private final String email;

    public JwtPrincipal(Long userId, String email) {
        this.userId = userId;
        this.email = email;
    }

    public Long getUserId() {
        return userId;
    }

    public String getEmail() {
        return email;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        
        if (o == null || getClass() != o.getClass()) return false;
        JwtPrincipal that = (JwtPrincipal) o;
        return Objects.equals(userId, that.userId) && Objects.equals(email, that.email);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, email);
    }
}
