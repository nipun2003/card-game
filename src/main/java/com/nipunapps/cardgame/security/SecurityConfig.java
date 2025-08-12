package com.nipunapps.cardgame.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nipunapps.cardgame.dto.enums.ErrorCode;
import com.nipunapps.cardgame.dto.enums.LoginErrorCode;
import com.nipunapps.cardgame.dto.response.BaseResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.security.authentication.*;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.session.InMemoryReactiveSessionRegistry;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.WebFilterExchange;
import org.springframework.security.web.server.authentication.SessionLimit;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.List;

@Configuration
@Slf4j
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomReactiveAuthenticationManager authenticationManager;
    private final ObjectMapper objectMapper;
    private final AuthenticationSuccessHandler successHandler;

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
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
                        .authenticationSuccessHandler(handlers ->
                                handlers.add(successHandler)
                        )
                        .authenticationFailureHandler(this::handleLoginFailure)
                )
                .authenticationManager(authenticationManager)
                .build();

    }

    private Mono<Void> handleLoginFailure(WebFilterExchange exchange, AuthenticationException exception) {
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

    private CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // Allow localhost:5173 (and other origins if needed)
        configuration.setAllowedOrigins(Arrays.asList(
                "http://localhost:5173",
                "https://localhost:5173" // Include HTTPS if needed
        ));

        // Allow all HTTP methods
        configuration.setAllowedMethods(Arrays.asList(
                "GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"
        ));

        // Allow all headers
        configuration.setAllowedHeaders(List.of("*"));

        // Allow credentials (important for your session-based auth)
        configuration.setAllowCredentials(true);

        // Cache preflight response for 1 hour
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }


}
