package com.nipunapps.cardgame.repository.impl;

import com.nipunapps.cardgame.repository.RoomPlayerTokenRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

@Repository
@Slf4j
public class InMemoryRoomPlayerTokenRepository implements RoomPlayerTokenRepository {

    /**
     * In-memory storage for player tokens, organized by room.
     * <p>
     * The outer map's key is the room ID, and its value is another map.
     * The inner map's key is the token, and its value is the associated player ID.
     * Thread-safe via {@link ConcurrentHashMap}.
     */
    private static final Map<String, Map<String, String>> roomTokens = new ConcurrentHashMap<>();

    @Override
    public Mono<Void> addPlayerToken(String roomId, String playerId, String token) {
        return Mono.fromRunnable(() -> roomTokens
                .computeIfAbsent(roomId, id -> new ConcurrentHashMap<>())
                .put(token, playerId));
    }

    @Override
    public Mono<Void> removeToken(String roomId, String token) {
        return Mono.fromRunnable(() -> {
            var tokens = roomTokens.get(roomId);
            if (tokens != null) {
                tokens.remove(token);
            }
        });
    }

    @Override
    public Mono<String> getPlayerIdWithToken(String roomId, String tokenId) {
        return Mono.fromSupplier(() -> {
            var tokens = roomTokens.get(roomId);
            if (tokens != null) {
                return tokens.get(tokenId);
            }
            return null;
        }).filter(Objects::nonNull);
    }

    @Override
    public Mono<String> getAndRemovePlayerIdWithToken(String roomId, String tokenId) {
        return Mono.fromSupplier(() -> {
            var tokens = roomTokens.get(roomId);
            if (tokens != null) {
                return tokens.remove(tokenId);
            }
            return null;
        }).filter(Objects::nonNull);
    }

    @Override
    public Mono<Void> clearRoomTokens(String roomId) {
        return Mono.fromRunnable(() -> roomTokens.remove(roomId));
    }
}
