package com.travelmanagementsystem.shared.exception;

import org.springframework.http.HttpStatus;

public class InvalidCredentialsException extends AuthenticationException {

    private InvalidCredentialsException() {
        super("INVALID_CREDENTIALS", "Invalid email or password", HttpStatus.UNAUTHORIZED);
    }

    public static InvalidCredentialsException of() {
        return new InvalidCredentialsException();
    }
}
