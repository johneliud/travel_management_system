package com.travelmanagementsystem.shared.exception;

import org.springframework.http.HttpStatus;

public class AccountDisabledException extends AuthenticationException {

    private AccountDisabledException(String status) {
        super("ACCOUNT_DISABLED", "Account is " + status.toLowerCase(), HttpStatus.FORBIDDEN);
    }

    public static AccountDisabledException of(String status) {
        return new AccountDisabledException(status);
    }
}
