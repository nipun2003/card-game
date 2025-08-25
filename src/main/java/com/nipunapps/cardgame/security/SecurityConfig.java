package com.nipunapps.cardgame.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.session.ReactiveSessionRegistry;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.authentication.*;
import org.springframework.security.web.server.context.ServerSecurityContextRepository;
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatchers;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;
import org.springframework.web.server.session.WebSessionStore;

import java.util.Arrays;
import java.util.List;

@Configuration
@Slf4j
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomAuthenticationEntryPoint authenticationEntryPoint;
    private final JsonBodyAuthenticationConverter authenticationConverter;
    private final CustomReactiveAuthenticationManager authenticationManager;
    private final AuthenticationSuccessHandler authenticationSuccessHandler;
    private final AuthenticationFailureHandler authenticationFailureHandler;
    private final ReactiveSessionRegistry sessionRegistry;
    private final ServerSecurityContextRepository securityContextRepository;
    private final WebSessionStore webSessionStore;

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        AuthenticationWebFilter loginFilter = new AuthenticationWebFilter(authenticationManager);
        loginFilter.setServerAuthenticationConverter(authenticationConverter);
        loginFilter.setRequiresAuthenticationMatcher(
                ServerWebExchangeMatchers.pathMatchers(HttpMethod.POST, "/auth/login")
        );

        // 1. Register the session when authentication succeeds
        RegisterSessionServerAuthenticationSuccessHandler registerHandler =
                new RegisterSessionServerAuthenticationSuccessHandler(sessionRegistry);

        // 2. Apply concurrency control
        ConcurrentSessionControlServerAuthenticationSuccessHandler concurrentHandler =
                new ConcurrentSessionControlServerAuthenticationSuccessHandler(
                        sessionRegistry,
                        new InvalidateLeastUsedServerMaximumSessionsExceededHandler(webSessionStore)
                );
        concurrentHandler.setSessionLimit(SessionLimit.of(3));

        // 3. Combine concurrency + your custom response
        DelegatingServerAuthenticationSuccessHandler successChain =
                new DelegatingServerAuthenticationSuccessHandler(
                        registerHandler,
                        concurrentHandler,
                        authenticationSuccessHandler // your JSON response logic
                );

        loginFilter.setAuthenticationSuccessHandler(successChain);
        loginFilter.setAuthenticationFailureHandler(authenticationFailureHandler);
        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .exceptionHandling(e -> e.authenticationEntryPoint(authenticationEntryPoint))
                .authorizeExchange(exchange -> {
                    exchange.pathMatchers("/").permitAll();
                    exchange.pathMatchers("/health").permitAll();
                    exchange.pathMatchers("/auth/**").permitAll();
                    exchange.pathMatchers("/ws/**").permitAll();
                    exchange.anyExchange().authenticated();
                })
                .securityContextRepository(securityContextRepository)
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
                .addFilterAt(loginFilter, SecurityWebFiltersOrder.AUTHENTICATION)
                .build();

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
