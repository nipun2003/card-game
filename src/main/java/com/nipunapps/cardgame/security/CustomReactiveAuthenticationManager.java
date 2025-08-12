package com.nipunapps.cardgame.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.ReactiveUserDetailsService;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Component
@RequiredArgsConstructor
@Slf4j
public class CustomReactiveAuthenticationManager implements ReactiveAuthenticationManager {

    private final ReactiveUserDetailsService userDetailsService;
    private final PasswordEncoder passwordEncoder;

    @Override
    public Mono<Authentication> authenticate(Authentication authentication) {
        final String username = authentication.getName();
        final String rawPassword = (String) authentication.getCredentials();

        return userDetailsService.findByUsername(username)
                .doFirst(() -> log.info("Attempting to authenticate user: {}", username))
                .switchIfEmpty(Mono.error(new UsernameNotFoundException(username)))
                .doOnNext(u -> log.info("User '{}' found in database", u.getUsername()))
                .flatMap(u -> verifyPassword(u.getPassword(), rawPassword).map(valid -> u))
                .filter(UserDetails::isEnabled)
                .switchIfEmpty(Mono.error(new DisabledException("The user is disabled, please contact support")))
                .filter(UserDetails::isAccountNonLocked)
                .switchIfEmpty(Mono.error(new LockedException("Account is locked, please contact support")))
                .filter(UserDetails::isAccountNonExpired)
                .switchIfEmpty(Mono.error(new AccountExpiredException("Account is expired, please contact support")))
                .filter(UserDetails::isCredentialsNonExpired)
                .switchIfEmpty(Mono.error(new CredentialsExpiredException("Credentials have expired, please reset your password")))
                .map(u -> new UsernamePasswordAuthenticationToken(u, null, u.getAuthorities()));
    }

    private Mono<Boolean> verifyPassword(String encodedPassword, String rawPassword) {
        return Mono.fromCallable(() -> {
                    if (!passwordEncoder.matches(rawPassword, encodedPassword)) {
                        throw new BadCredentialsException("Username or password is incorrect");
                    }
                    return true;
                })
                .subscribeOn(Schedulers.boundedElastic());
    }
}
