package com.nipunapps.cardgame.dto.enums;

public interface ErrorCode {
    String getCode();
    String getMessage();
    int getHttpStatus();
}
