package com.travelmanagementsystem.shared.exception;

import org.springframework.http.HttpStatus;

public class AuthenticationException extends DomainException {

    private final HttpStatus httpStatus;

    protected AuthenticationException(String errorCode, String message, HttpStatus httpStatus) {
        super(errorCode, message);
        this.httpStatus = httpStatus;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }
}
