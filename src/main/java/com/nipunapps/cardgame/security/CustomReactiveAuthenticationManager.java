package com.nipunapps.cardgame.security;

import com.nipunapps.cardgame.exception.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.ReactiveUserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
@Slf4j
public class CustomReactiveAuthenticationManager implements ReactiveAuthenticationManager {

    private final ReactiveUserDetailsService userDetailsService;
    private final PasswordEncoder passwordEncoder;

    @Override
    public Mono<Authentication> authenticate(Authentication authentication) {
        String username = authentication.getName();
        log.info("Authenticating user: {}", username);
        String presentPassword = (String) authentication.getCredentials();
        return userDetailsService.findByUsername(username)
                .switchIfEmpty(Mono.error(new UsernameNotFoundException(username)))
                .flatMap(userDetails -> {
                    if (!passwordEncoder.matches(presentPassword, userDetails.getPassword())) {
                        return Mono.error(new BadCredentialException());
                    }
                    if (!userDetails.isEnabled()) {
                        return Mono.error(new AccountDisabledException());
                    }

                    if (!userDetails.isAccountNonLocked()) {
                        return Mono.error(new AccountDisabledException("Account is locked"));
                    }

                    if (!userDetails.isAccountNonExpired()) {
                        return Mono.error(new AccExpiredException());
                    }

                    if (!userDetails.isCredentialsNonExpired()) {
                        return Mono.error(new CredentialExpiredException("Credentials have expired"));
                    }

                    UsernamePasswordAuthenticationToken result = new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null,
                            userDetails.getAuthorities()
                    );
                    return Mono.just(result);
                });
    }
}
