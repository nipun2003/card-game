package com.nipunapps.cardgame.exception;

import com.nipunapps.cardgame.exception.standardexception.UnauthorizedException;

public class BadCredentialException extends UnauthorizedException {

    public BadCredentialException() {
        super("Invalid credentials provided");
    }

}
