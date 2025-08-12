package com.nipunapps.cardgame.exception;

import com.nipunapps.cardgame.exception.standardexception.UnauthorizedException;

public class AccountDisabledException extends UnauthorizedException {

    public AccountDisabledException() {
        super("Account is disabled");
    }

    public AccountDisabledException(String message) {
        super(message);
    }

    public AccountDisabledException(String message, Throwable cause) {
        super(message, cause);
    }

    public AccountDisabledException(Throwable cause) {
        super(cause.getMessage(), cause);
    }
}
