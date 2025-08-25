package com.nipunapps.cardgame.games.dragontiger;

import com.nipunapps.cardgame.exception.UserNotFoundException;
import com.nipunapps.cardgame.games.exception.NoTokenFoundException;
import com.nipunapps.cardgame.games.exception.RoomFullException;
import com.nipunapps.cardgame.models.PlayerModel;
import com.nipunapps.cardgame.repository.RoomPlayerRepository;
import com.nipunapps.cardgame.repository.RoomPlayerTokenRepository;
import com.nipunapps.cardgame.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class DragonTigerPlayerManagement {

    private final RoomPlayerRepository playerRepository;
    private final RoomPlayerTokenRepository tokenRepository;
    private final UserRepository userRepository;


    public Mono<PlayerModel> addPlayer(
            String roomId,
            String tokenId,
            WebSocketSession session
    ) {
        return playerRepository.findAllPlayersByRoomId(roomId)
                .flatMap(players -> {
                    if (players.size() >= DragonTigerArena.MAX_PLAYERS) {
                        return Mono.error(new RoomFullException(roomId));
                    } else {
                        return Mono.empty();
                    }
                }).then(tokenRepository.getAndRemovePlayerIdWithToken(roomId, tokenId))
                .switchIfEmpty(Mono.error(new NoTokenFoundException(tokenId, roomId)))
                .flatMap(userRepository::findById)
                .switchIfEmpty(Mono.error(new UserNotFoundException("User not found for tokenId: " + tokenId)))
                .flatMap(user -> {
                    final var player = PlayerModel.fromUserEntity(user, session);
                    return playerRepository.savePlayerToRoom(player, roomId)
                            .thenReturn(player);
                });
    }

    public Mono<Void> removePlayer(String roomId, String playerId) {
        return playerRepository.removePlayerFromRoom(playerId, roomId);
    }
}
