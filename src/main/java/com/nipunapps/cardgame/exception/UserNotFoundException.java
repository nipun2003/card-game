package com.nipunapps.cardgame.exception;

import com.nipunapps.cardgame.exception.standardexception.UnauthorizedException;

public class UserNotFoundException extends UnauthorizedException {

    public UserNotFoundException(String userId) {
        super("User with ID: " + userId + " not found.");
    }

    public UserNotFoundException(String userId, Throwable cause) {
        super("User with ID: " + userId + " not found.", cause);
    }
}
