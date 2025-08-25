package com.nipunapps.cardgame.sockets;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Mono;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketMessageSender {

    private final ObjectMapper mapper;

    public Mono<Void> sendMessage(WebSocketSession session, Object message) {
        return Mono.fromCallable(() -> mapper.writeValueAsString(message))
                .map(session::textMessage)
                .flatMap(wsMessage -> session.send(Mono.just(wsMessage)))
                .doOnError(JsonProcessingException.class, e ->
                        log.warn("Failed to serialize message for session [{}]: {}", session.getId(), e.getMessage()))
                .doOnError(e ->
                        log.error("Unexpected error while sending message to session [{}]", session.getId(), e))
                .onErrorResume(e -> Mono.empty()); // swallow error, don’t propagate
    }
}
