package com.travelmanagementsystem.shared.exception;

import org.springframework.http.HttpStatus;

public class InvalidRefreshTokenException extends AuthenticationException {

    private InvalidRefreshTokenException(String message) {
        super("INVALID_REFRESH_TOKEN", message, HttpStatus.UNAUTHORIZED);
    }

    public static InvalidRefreshTokenException of(String message) {
        return new InvalidRefreshTokenException(message);
    }

    public static InvalidRefreshTokenException expired() {
        return new InvalidRefreshTokenException("Refresh token has expired");
    }

    public static InvalidRefreshTokenException revoked() {
        return new InvalidRefreshTokenException("Refresh token has been revoked");
    }

    public static InvalidRefreshTokenException notFound() {
        return new InvalidRefreshTokenException("Invalid refresh token");
    }
}
