package com.nipunapps.cardgame.exception;

import com.nipunapps.cardgame.exception.standardexception.UnauthorizedException;

public class AccExpiredException extends UnauthorizedException {

    public AccExpiredException() {
        super("Account has expired");
    }

    public AccExpiredException(String message) {
        super(message);
    }

    public AccExpiredException(String message, Throwable cause) {
        super(message, cause);
    }

    public AccExpiredException(Throwable cause) {
        super(cause.getMessage(),cause);
    }
}
