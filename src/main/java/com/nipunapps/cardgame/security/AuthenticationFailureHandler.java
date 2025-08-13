package com.nipunapps.cardgame.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nipunapps.cardgame.dto.enums.ErrorCode;
import com.nipunapps.cardgame.dto.enums.LoginErrorCode;
import com.nipunapps.cardgame.dto.response.BaseResponse;
import com.nipunapps.cardgame.exception.BadLoginRequestFormat;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.security.authentication.*;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.server.WebFilterExchange;
import org.springframework.security.web.server.authentication.ServerAuthenticationFailureHandler;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class AuthenticationFailureHandler implements ServerAuthenticationFailureHandler {

    private final ObjectMapper objectMapper;

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
        } else if (exception instanceof BadLoginRequestFormat e) {
            final var code = LoginErrorCode.INVALID_REQUEST;
            code.setMessage(e.getMessage());
            errorCode = code;
        } else {
            errorCode = LoginErrorCode.AUTH_FAILED;
        }
        return errorCode;
    }

    @Override
    public Mono<Void> onAuthenticationFailure(WebFilterExchange exchange, AuthenticationException exception) {
        ServerHttpResponse response = exchange.getExchange().getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().add("Content-Type", "application/json");

        ErrorCode errorCode = getAuthErrorCode(exception);

        response.setStatusCode(HttpStatus.valueOf(errorCode.getHttpStatus()));

        final var responseData = new BaseResponse<>(errorCode);
        String jsonErrorResponse;
        try {
            jsonErrorResponse = objectMapper.writeValueAsString(responseData);

        } catch (Exception e) {
            jsonErrorResponse = "{\"success\": false, \"message\": \"An error occurred while processing the error response\", \"errorCode\": \"INTERNAL_ERROR\"}";
        }

        DataBuffer buffer = response.bufferFactory().wrap(jsonErrorResponse.getBytes());
        return response.writeWith(Mono.just(buffer));
    }
}
