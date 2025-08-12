package com.nipunapps.cardgame.exception;

import com.nipunapps.cardgame.exception.standardexception.UnauthorizedException;

public class CredentialExpiredException extends UnauthorizedException {

    public CredentialExpiredException() {
        super("Credentials have expired. Please update your credentials.");
    }

    public CredentialExpiredException(String message) {
        super(message);
    }
}
