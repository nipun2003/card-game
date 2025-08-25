package com.nipunapps.cardgame.games.dragontiger;

import com.nipunapps.cardgame.games.dragontiger.model.DragonTigerJoinResult;
import com.nipunapps.cardgame.models.PlayerModel;
import reactor.core.publisher.Mono;

public interface DragonTigerArena {


    Mono<Boolean> addPlayerToken(String token, String playerId);

    Mono<DragonTigerJoinResult> join(String token, PlayerModel player);

    Mono<Boolean> destroyArena();
}
