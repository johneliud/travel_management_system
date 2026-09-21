package com.travelmanagementsystem.shared.exception;

import org.springframework.http.HttpStatus;

public class InvalidPasswordException extends AuthenticationException {

    private InvalidPasswordException() {
        super("INVALID_PASSWORD", "Current password is incorrect", HttpStatus.UNAUTHORIZED);
    }

    public static InvalidPasswordException of() {
        return new InvalidPasswordException();
    }
}
