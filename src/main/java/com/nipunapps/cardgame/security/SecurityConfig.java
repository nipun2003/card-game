package com.nipunapps.cardgame.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.session.InMemoryReactiveSessionRegistry;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.WebFilterExchange;
import org.springframework.security.web.server.authentication.SessionLimit;
import reactor.core.publisher.Mono;

@Configuration
@Slf4j
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomReactiveAuthenticationManager authenticationManager;

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .authorizeExchange(exchange -> {
                    exchange.pathMatchers("/").permitAll();
                    exchange.pathMatchers("/health").permitAll();
                    exchange.pathMatchers("/auth/**").permitAll();
                    exchange.anyExchange().authenticated();
                })
                .sessionManagement(session -> session
                        .concurrentSessions(c -> c
                                .maximumSessions(SessionLimit.of(3))
                                .sessionRegistry(new InMemoryReactiveSessionRegistry())
                        )
                )
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
                .formLogin(login -> login
                        .loginPage("/auth/login")
                        .authenticationSuccessHandler(handlers -> {
                            handlers.add(new AuthenticationSuccessHandler());
                        })
                        .authenticationFailureHandler(this::handleLoginFailure)
                )
                .authenticationManager(authenticationManager)
                .build();

    }

    private Mono<Void> handleLoginFailure(WebFilterExchange exchange, AuthenticationException exception) {
        ServerHttpResponse response = exchange.getExchange().getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().add("Content-Type", "application/json");

        String jsonResponse = String.format(
                "{\"status\":\"error\",\"message\":\"%s\"}",
                exception.getMessage()
        );

        DataBuffer buffer = response.bufferFactory().wrap(jsonResponse.getBytes());
        return response.writeWith(Mono.just(buffer));
    }


}
