package com.nipunapps.cardgame.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nipunapps.cardgame.dto.response.BaseResponse;
import com.nipunapps.cardgame.dto.response.auth.LoginResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.session.ReactiveSessionRegistry;
import org.springframework.security.web.server.WebFilterExchange;
import org.springframework.security.web.server.authentication.ServerAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuthenticationSuccessHandler implements ServerAuthenticationSuccessHandler {

    private final ObjectMapper objectMapper;
    private final ReactiveSessionRegistry sessionRegistry;

    @Override
    public Mono<Void> onAuthenticationSuccess(WebFilterExchange exchange, Authentication authentication) {
        return sessionRegistry.getAllSessions(authentication.getPrincipal())
                .collectList()
                .doOnSuccess(sessions -> {
                    log.info("User {} authenticated successfully with {} active sessions.",
                            authentication.getName(), sessions.size());
                })
                .then(Mono.defer(() -> {
                    ServerHttpResponse response = exchange.getExchange().getResponse();
                    response.setStatusCode(HttpStatus.OK);
                    response.getHeaders().add("Content-Type", "application/json");
                    response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

                    final var userData = LoginResponseDto.builder()
                            .username(authentication.getName())
                            .roles(authentication.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList()
                            ).build();
                    final var responseData = BaseResponse.<LoginResponseDto>builder()
                            .success(true)
                            .data(userData)
                            .build();

                    String errorResponse;
                    try {
                        errorResponse = objectMapper.writeValueAsString(responseData);
                    } catch (Exception e) {
                        errorResponse = "{\"success\":false,\"message\":\"Internal Server Error\",\"errorCode\":\"500\"}";
                        response.setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR);
                    }

                    DataBuffer buffer = response.bufferFactory().wrap(errorResponse.getBytes());
                    return response.writeWith(Mono.just(buffer));
                }));
    }
}