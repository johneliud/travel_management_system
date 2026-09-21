package com.travelmanagementsystem.shared.security;

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
    public boolean equals(Object obj) {
        if (this == obj) return true;
        
        if (obj == null || getClass() != obj.getClass()) return false;
        JwtPrincipal that = (JwtPrincipal) obj;
        return Objects.equals(userId, that.userId) && Objects.equals(email, that.email);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, email);
    }
}
