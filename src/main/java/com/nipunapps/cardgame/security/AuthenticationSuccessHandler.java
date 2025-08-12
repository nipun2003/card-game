package com.nipunapps.cardgame.security;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.server.WebFilterExchange;
import org.springframework.security.web.server.authentication.ServerAuthenticationSuccessHandler;
import reactor.core.publisher.Mono;

import java.util.stream.Collectors;

public class AuthenticationSuccessHandler implements ServerAuthenticationSuccessHandler {

    @Override
    public Mono<Void> onAuthenticationSuccess(WebFilterExchange exchange, Authentication authentication) {
        ServerHttpResponse response = exchange.getExchange().getResponse();
        response.setStatusCode(HttpStatus.OK);
        response.getHeaders().add("Content-Type", "application/json");

        String jsonResponse = String.format(
                "{\"status\":\"success\",\"user\":\"%s\",\"authorities\":[%s]}",
                authentication.getName(),
                authentication.getAuthorities().stream()
                        .map(auth -> "\"" + auth.getAuthority() + "\"")
                        .collect(Collectors.joining(","))
        );

        DataBuffer buffer = response.bufferFactory().wrap(jsonResponse.getBytes());
        return response.writeWith(Mono.just(buffer));
    }
}
