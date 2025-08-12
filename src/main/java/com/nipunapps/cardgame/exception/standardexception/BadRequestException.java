package com.nipunapps.cardgame.exception.standardexception;

public class BadRequestException extends RuntimeException{

    public BadRequestException() {
        super("Bad Request");
    }

    public BadRequestException(String message) {
        super(message);
    }

    public BadRequestException(String message, Throwable cause) {
        super(message, cause);
    }

    public BadRequestException(Throwable cause) {
        super(cause.getMessage(), cause);
    }
}
