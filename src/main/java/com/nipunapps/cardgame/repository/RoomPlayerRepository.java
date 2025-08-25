package com.nipunapps.cardgame.repository;

import com.nipunapps.cardgame.models.PlayerModel;
import reactor.core.publisher.Mono;

import java.util.List;

public interface RoomPlayerRepository {

    Mono<List<PlayerModel>> findAllPlayersByRoomId(String roomId);

    Mono<PlayerModel> findPlayerByIdAndRoomId(String playerId, String roomId);

    Mono<Void> savePlayerToRoom(PlayerModel player, String roomId);

    Mono<Void> removePlayerFromRoom(String playerId, String roomId);

    Mono<Void> clearRoom(String roomId);
}
