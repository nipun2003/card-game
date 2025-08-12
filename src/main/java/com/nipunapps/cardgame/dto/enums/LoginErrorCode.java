package com.nipunapps.cardgame.dto.enums;

public enum LoginErrorCode implements ErrorCode {
    INVALID_CREDENTIALS("INVALID_CREDENTIALS", "Invalid username or password", 401),
    ACCOUNT_LOCKED("ACCOUNT_LOCKED", "Your account has been locked. Please contact support.", 403),
    ACCOUNT_DISABLED("ACCOUNT_DISABLED", "Your account has been disabled. Please contact support.", 403),
    ACCOUNT_EXPIRED("ACCOUNT_EXPIRED", "Your account has expired. Please contact support.", 403),
    CREDENTIALS_EXPIRED("CREDENTIALS_EXPIRED", "Your credentials have expired. Please reset your password.", 403),
    USER_NOT_FOUND("USER_NOT_FOUND", "User not found", 404),
    AUTH_FAILED("AUTH_FAILED", "Authentication failed", 401);

    private final String code;
    private final String message;
    private final int httpStatus;

    LoginErrorCode(String code, String message, int httpStatus) {
        this.code = code;
        this.message = message;
        this.httpStatus = httpStatus;
    }

    @Override public String getCode() { return code; }
    @Override public String getMessage() { return message; }
    @Override public int getHttpStatus() { return httpStatus; }
}
