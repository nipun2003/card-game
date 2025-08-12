package com.nipunapps.cardgame.security;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.session.ReactiveSessionInformation;
import org.springframework.security.core.session.ReactiveSessionRegistry;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class CustomReactiveSessionRegistry implements ReactiveSessionRegistry {

    @Override
    public Flux<ReactiveSessionInformation> getAllSessions(Object principal) {
        return null;
    }

    @Override
    public Mono<Void> saveSessionInformation(ReactiveSessionInformation information) {
        return null;
    }

    @Override
    public Mono<ReactiveSessionInformation> getSessionInformation(String sessionId) {
        return null;
    }

    @Override
    public Mono<ReactiveSessionInformation> removeSessionInformation(String sessionId) {
        return null;
    }

    @Override
    public Mono<ReactiveSessionInformation> updateLastAccessTime(String sessionId) {
        return null;
    }
}
