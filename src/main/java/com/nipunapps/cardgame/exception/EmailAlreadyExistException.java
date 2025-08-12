package com.nipunapps.cardgame.exception;


import com.nipunapps.cardgame.exception.standardexception.BadRequestException;

public class EmailAlreadyExistException extends BadRequestException {

    public EmailAlreadyExistException(String email) {
        super("The email '" + email + "' is already in use.");
    }
}
