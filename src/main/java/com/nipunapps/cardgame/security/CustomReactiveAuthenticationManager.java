package com.nipunapps.cardgame.security;

import com.nipunapps.cardgame.exception.UsernameNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.*;
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
        String presentPassword = (String) authentication.getCredentials();
        return userDetailsService.findByUsername(username)
                .doOnSuccess((d) -> {
                    log.info("Username {} found", d.getUsername());
                })
                .switchIfEmpty(Mono.error(new UsernameNotFoundException(username)))
                .flatMap(userDetails -> {
                    if (!passwordEncoder.matches(presentPassword, userDetails.getPassword())) {
                        return Mono.error(new BadCredentialsException("Username or password is incorrect"));
                    }
                    if (!userDetails.isEnabled()) {
                        return Mono.error(new DisabledException("The user is disabled, please contact support"));
                    }

                    if (!userDetails.isAccountNonLocked()) {
                        return Mono.error(new LockedException("Account is locked, please contact support"));
                    }

                    if (!userDetails.isAccountNonExpired()) {
                        return Mono.error(new AccountExpiredException("Account is expired, please contact support"));
                    }

                    if (!userDetails.isCredentialsNonExpired()) {
                        return Mono.error(new CredentialsExpiredException("Credentials have expired, please reset your password"));
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
