package com.nipunapps.cardgame.service;

import com.nipunapps.cardgame.models.PlayerModel;
import com.nipunapps.cardgame.repository.RoomPlayerRepository;
import com.nipunapps.cardgame.sockets.WebSocketMessageSender;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class RoomEventSender {

    private final RoomPlayerRepository repository;
    private final WebSocketMessageSender messageSender;

    /**
     * Broadcasts a message to all players in the room.
     * Throws {@link RoomEventException} if a player is missing or session is null.
     */
    public Mono<Void> broadcastMessage(String roomId, Object message) {
        return repository.findAllPlayersByRoomId(roomId)
                .flatMap(players -> {
                    if (players.isEmpty()) {
                        return Mono.error(new RoomEventException(
                                "No players found in room " + roomId));
                    }
                    return Flux.fromIterable(players)
                            .flatMap(player -> sendMessageToPlayerSafe(player, message))
                            .then();
                });
    }

    public Mono<Void> broadcastMessageToSeated(String roomId, Object message) {
        return repository.findAllPlayersSeatedPlayerByRoomId(roomId)
                .flatMap(players -> {
                    if (players.isEmpty()) {
                        return Mono.error(new RoomEventException(
                                "No players found in room " + roomId));
                    }
                    return Flux.fromIterable(players)
                            .flatMap(player -> sendMessageToPlayerSafe(player, message))
                            .then();
                });
    }

    /**
     * Sends a message to a specific player.
     * Throws {@link RoomEventException} if the player is not found or session is null.
     */
    public Mono<Void> sendMessageToPlayer(String roomId, String playerId, Object message) {
        return repository.findPlayerByIdAndRoomId(playerId, roomId)
                .switchIfEmpty(Mono.error(new RoomEventException(
                        "Player " + playerId + " not found in room " + roomId)))
                .flatMap(player -> sendMessageToPlayerSafe(player, message));
    }

    /**
     * Helper method to safely send a message to a player,
     * checking if the WebSocketSession is present.
     */
    private Mono<Void> sendMessageToPlayerSafe(PlayerModel player, Object message) {
        if (player.getSession() == null) {
            return Mono.error(new RoomEventException(
                    "Player " + player.getId() + " has no active WebSocket session"));
        }
        return messageSender.sendMessage(player.getSession(), message);
    }

    /**
     * Custom exception for RoomEventSender failures.
     */
    public static class RoomEventException extends RuntimeException {
        public RoomEventException(String message) {
            super(message);
        }

        public RoomEventException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
