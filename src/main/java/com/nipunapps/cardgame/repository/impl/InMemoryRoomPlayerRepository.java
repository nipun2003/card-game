package com.nipunapps.cardgame.repository.impl;

import com.nipunapps.cardgame.models.PlayerModel;
import com.nipunapps.cardgame.repository.RoomPlayerRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Repository
@Slf4j
public class InMemoryRoomPlayerRepository implements RoomPlayerRepository {

    // Map<roomId, Map<playerId, PlayerModel>>
    private final ConcurrentMap<String, ConcurrentMap<String, PlayerModel>> roomPlayers = new ConcurrentHashMap<>();

    @Override
    public Mono<List<PlayerModel>> findAllPlayersByRoomId(String roomId) {
        return Mono.fromSupplier(() ->
                List.copyOf(roomPlayers
                        .computeIfAbsent(roomId, id -> new ConcurrentHashMap<>())
                        .values())
        );
    }

    @Override
    public Mono<PlayerModel> findPlayerByIdAndRoomId(String playerId, String roomId) {
        return Mono.fromSupplier(() ->
                roomPlayers
                        .computeIfAbsent(roomId, id -> new ConcurrentHashMap<>())
                        .get(playerId)
        ).filter(Objects::nonNull);
    }

    @Override
    public Mono<Void> savePlayerToRoom(PlayerModel player, String roomId) {
        return Mono.fromRunnable(() -> {
            roomPlayers
                    .computeIfAbsent(roomId, id -> new ConcurrentHashMap<>())
                    .put(player.getId(), player);
        });
    }

    @Override
    public Mono<Void> removePlayerFromRoom(String playerId, String roomId) {
        return Mono.fromRunnable(() -> {
            var players = roomPlayers.get(roomId);
            if (players != null) {
                players.remove(playerId);
            }
        });
    }

    @Override
    public Mono<Void> clearRoom(String roomId) {
        return Mono.fromRunnable(() -> roomPlayers.remove(roomId));
    }
}
