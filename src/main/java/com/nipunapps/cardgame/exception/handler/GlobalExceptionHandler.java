package com.nipunapps.cardgame.exception.handler;

import com.nipunapps.cardgame.dto.enums.ErrorCode;
import com.nipunapps.cardgame.dto.enums.LoginErrorCode;
import com.nipunapps.cardgame.dto.response.BaseResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.*;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import reactor.core.publisher.Mono;


@RestControllerAdvice
public class GlobalExceptionHandler {

    private static ErrorCode getAuthErrorCode(AuthenticationException exception) {
        ErrorCode errorCode;

        if (exception instanceof LockedException) {
            errorCode = LoginErrorCode.ACCOUNT_LOCKED;
        } else if (exception instanceof DisabledException) {
            errorCode = LoginErrorCode.ACCOUNT_DISABLED;
        } else if (exception instanceof BadCredentialsException) {
            errorCode = LoginErrorCode.INVALID_CREDENTIALS;
        } else if (exception instanceof UsernameNotFoundException) {
            errorCode = LoginErrorCode.USER_NOT_FOUND;
        } else if (exception instanceof CredentialsExpiredException) {
            errorCode = LoginErrorCode.CREDENTIALS_EXPIRED;
        } else if (exception instanceof AccountExpiredException) {
            errorCode = LoginErrorCode.ACCOUNT_EXPIRED;
        } else {
            errorCode = LoginErrorCode.AUTH_FAILED;
        }
        return errorCode;
    }


    @ExceptionHandler(AuthenticationException.class)
    public Mono<ResponseEntity<BaseResponse<?>>> handleAuthenticationException(AuthenticationException ex) {
        final var errorCode = getAuthErrorCode(ex);
        return Mono.just(
                ResponseEntity
                        .status(errorCode.getHttpStatus())
                        .body(new BaseResponse<>(errorCode))
        );
    }
}
