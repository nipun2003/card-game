package com.nipunapps.cardgame.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nipunapps.cardgame.dto.enums.CommonErrorCode;
import com.nipunapps.cardgame.dto.response.BaseResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.server.ServerAuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Slf4j
@Component
@RequiredArgsConstructor
public class CustomAuthenticationEntryPoint implements ServerAuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    @Override
    public Mono<Void> commence(ServerWebExchange exchange, AuthenticationException ex) {
        ServerHttpResponse response = exchange.getResponse();
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        final var responseData = new BaseResponse<>(CommonErrorCode.UNAUTHORIZED);
        String responseBody;
        try {
            responseBody = objectMapper.writeValueAsString(responseData);
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
        } catch (Exception e) {
            log.info("Error while writing error response: {}", e.getMessage());
            responseBody = "{\"success\": false, \"message\": \"An error occurred while processing the error response\", \"errorCode\": \"INTERNAL_ERROR\"}";
            response.setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR);
        }
        return response.writeWith(Mono.just(response.bufferFactory().wrap(responseBody.getBytes())));
    }
}
