package com.nipunapps.cardgame.exception;

import com.nipunapps.cardgame.exception.standardexception.NotFoundException;

public class UsernameNotFoundException extends NotFoundException {

    public UsernameNotFoundException(String username) {
        super("Username not found: " + username);
    }
}
