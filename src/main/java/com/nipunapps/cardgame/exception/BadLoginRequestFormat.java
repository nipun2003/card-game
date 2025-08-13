package com.nipunapps.cardgame.exception;

import org.springframework.security.core.AuthenticationException;

public class BadLoginRequestFormat extends AuthenticationException {

    public BadLoginRequestFormat(String msg) {
        super(msg);
    }

    public BadLoginRequestFormat(String msg, Throwable cause) {
        super(msg, cause);
    }
}
