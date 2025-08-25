package com.nipunapps.cardgame.repository;

import reactor.core.publisher.Mono;

public interface RoomPlayerTokenRepository {

    Mono<Void> addPlayerToken(String roomId, String playerId, String token);

    Mono<Void> removeToken(String roomId, String token);

    Mono<String> getPlayerIdWithToken(String roomId, String tokenId);

    Mono<String> getAndRemovePlayerIdWithToken(String roomId, String tokenId);

    Mono<Void> clearRoomTokens(String roomId);
}
