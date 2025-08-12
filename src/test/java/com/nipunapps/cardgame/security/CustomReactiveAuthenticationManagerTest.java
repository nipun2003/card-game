package com.nipunapps.cardgame.security;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.ReactiveUserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomReactiveAuthenticationManagerTest {

    private static final String USERNAME = "testUser";
    private static final String RAW_PASSWORD = "secret";
    private static final String ENCODED_PASSWORD = "{bcrypt}encoded_secret";

    @Mock
    private ReactiveUserDetailsService userDetailsService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private CustomReactiveAuthenticationManager authenticationManager;

    private Authentication createAuthToken(String username, String password) {
        return new UsernamePasswordAuthenticationToken(username, password);
    }

    private CustomUserDetails createUser(boolean enabled, boolean locked, boolean credentialsExpired) {
        return CustomUserDetails.builder()
                .username(USERNAME)
                .password(ENCODED_PASSWORD)
                .roles(List.of("USER"))
                .enabled(enabled)
                .locked(locked)
                .credentialsExpired(credentialsExpired)
                .build();
    }

    @Test
    void authenticate_success() {
        CustomUserDetails user = createUser(true, false, false);

        when(userDetailsService.findByUsername(USERNAME)).thenReturn(Mono.just(user));
        when(passwordEncoder.matches(RAW_PASSWORD, ENCODED_PASSWORD)).thenReturn(true);

        StepVerifier.create(authenticationManager.authenticate(createAuthToken(USERNAME, RAW_PASSWORD)))
                .expectNextMatches(auth -> {
                    CustomUserDetails details = (CustomUserDetails) auth.getPrincipal();
                    return details.getUsername().equals(USERNAME) &&
                            auth.getAuthorities().size() == 1;
                })
                .verifyComplete();

        verify(userDetailsService).findByUsername(USERNAME);
        verify(passwordEncoder).matches(RAW_PASSWORD, ENCODED_PASSWORD);
    }


    @Test
    void authenticate_userNotFound() {
        when(userDetailsService.findByUsername(USERNAME)).thenReturn(Mono.empty());

        StepVerifier.create(authenticationManager.authenticate(createAuthToken(USERNAME, RAW_PASSWORD)))
                .expectError(UsernameNotFoundException.class)
                .verify();
    }

    @Test
    void authenticate_badPassword() {
        CustomUserDetails user = createUser(true, false, false);

        when(userDetailsService.findByUsername(USERNAME)).thenReturn(Mono.just(user));
        when(passwordEncoder.matches(RAW_PASSWORD, ENCODED_PASSWORD)).thenReturn(false);

        StepVerifier.create(authenticationManager.authenticate(createAuthToken(USERNAME, RAW_PASSWORD)))
                .expectError(BadCredentialsException.class)
                .verify();
    }

    @Test
    void authenticate_disabledUser() {
        CustomUserDetails user = createUser(false, false, false);

        when(userDetailsService.findByUsername(USERNAME)).thenReturn(Mono.just(user));
        when(passwordEncoder.matches(RAW_PASSWORD, ENCODED_PASSWORD)).thenReturn(true);

        StepVerifier.create(authenticationManager.authenticate(createAuthToken(USERNAME, RAW_PASSWORD)))
                .expectError(DisabledException.class)
                .verify();
    }

    @Test
    void authenticate_lockedUser() {
        CustomUserDetails user = createUser(true, true, false);

        when(userDetailsService.findByUsername(USERNAME)).thenReturn(Mono.just(user));
        when(passwordEncoder.matches(RAW_PASSWORD, ENCODED_PASSWORD)).thenReturn(true);

        StepVerifier.create(authenticationManager.authenticate(createAuthToken(USERNAME, RAW_PASSWORD)))
                .expectError(LockedException.class)
                .verify();
    }

    @Test
    void authenticate_accountExpired() {
        // Simulate account expired by making isAccountNonExpired return false
        CustomUserDetails user = spy(createUser(true, false, false));
        when(user.isAccountNonExpired()).thenReturn(false);

        when(userDetailsService.findByUsername(USERNAME)).thenReturn(Mono.just(user));
        when(passwordEncoder.matches(RAW_PASSWORD, ENCODED_PASSWORD)).thenReturn(true);

        StepVerifier.create(authenticationManager.authenticate(createAuthToken(USERNAME, RAW_PASSWORD)))
                .expectError(AccountExpiredException.class)
                .verify();
    }

    @Test
    void authenticate_credentialsExpired() {
        CustomUserDetails user = createUser(true, false, true);

        when(userDetailsService.findByUsername(USERNAME)).thenReturn(Mono.just(user));
        when(passwordEncoder.matches(RAW_PASSWORD, ENCODED_PASSWORD)).thenReturn(true);

        StepVerifier.create(authenticationManager.authenticate(createAuthToken(USERNAME, RAW_PASSWORD)))
                .expectError(CredentialsExpiredException.class)
                .verify();
    }

}