package com.nipunapps.cardgame.games.dragontiger;

import com.nipunapps.cardgame.games.dragontiger.model.DragonTigerJoinResult;
import com.nipunapps.cardgame.models.PlayerModel;
import reactor.core.publisher.Mono;

public interface DragonTigerArena {

    /**
     * Maximum number of state.getPlayers() allowed in this room.
     */
    int MAX_PLAYERS = 5;
    int MAX_IDLE_SECONDS = 90;
    int GAME_START_DELAY_SECONDS = 5;

    Mono<Boolean> addPlayerToken(String token, String playerId);

    Mono<DragonTigerJoinResult> join(String token, PlayerModel player);

    void destroyArena();
}
