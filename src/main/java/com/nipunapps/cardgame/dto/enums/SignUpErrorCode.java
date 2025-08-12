package com.nipunapps.cardgame.dto.enums;

public enum SignUpErrorCode implements ErrorCode {
    USERNAME_TAKEN("SIGNUP_001", "Username already taken", 400),
    EMAIL_INVALID("SIGNUP_002", "Email format is invalid", 400),
    PASSWORD_WEAK("SIGNUP_003", "Password does not meet requirements", 400);

    private final String code;
    private final String message;
    private final int httpStatus;

    SignUpErrorCode(String code, String message, int httpStatus) {
        this.code = code;
        this.message = message;
        this.httpStatus = httpStatus;
    }

    @Override public String getCode() { return code; }
    @Override public String getMessage() { return message; }
    @Override public int getHttpStatus() { return httpStatus; }
}
