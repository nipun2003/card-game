package com.nipunapps.cardgame.games.dragontiger;

import com.nipunapps.cardgame.enums.GameLifecycle;
import com.nipunapps.cardgame.games.dragontiger.enums.DragonTigerPhase;
import com.nipunapps.cardgame.games.dragontiger.model.DragonTigerPlayerBet;
import com.nipunapps.cardgame.models.PlayerModel;
import lombok.Getter;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

public class DragonTigerArenaState {

    /**
     * Active players in the room.
     * <p>
     * Key: playerId <br>
     * Value: {@link PlayerModel} instance representing the player.
     * </p>
     */
    @Getter
    private final Map<String, PlayerModel> players = new ConcurrentHashMap<>();

    /**
     * Current bets placed by players in the room.
     * <p>
     * Key: playerId <br>
     * Value: {@link DragonTigerPlayerBet} representing the bet details.
     * </p>
     *
     * <h3>Lifecycle Notes:</h3>
     * <ul>
     *   <li>Each player can have at most one active bet per round.</li>
     *   <li>This map <b>must be cleared</b> before a new round starts, or
     *       immediately after the round ends when results are calculated.</li>
     *   <li>Clearing ensures that stale bets from the previous round
     *       do not interfere with subsequent rounds.</li>
     * </ul>
     */
    @Getter
    private final Map<String, DragonTigerPlayerBet> playerBets = new ConcurrentHashMap<>();

    /**
     * One-time join tokens issued to players.
     * <p>
     * Key: playerId <br>
     * Value: token string.
     * </p>
     * <p>
     * A token is required for a player to join the room successfully.
     * Once validated, the token is removed (invalidated) to prevent reuse.
     * </p>
     */
    @Getter
    private final Map<String, String> playerTokens = new ConcurrentHashMap<>();

    /**
     * High-level lifecycle of the game (e.g., WAITING, RUNNING, DESTROYED).
     */
    private final AtomicReference<GameLifecycle> lifecycle =
            new AtomicReference<>(GameLifecycle.IDLE);

    /**
     * Detailed round phase of the game (only valid when lifecycle is RUNNING).
     */
    private final AtomicReference<DragonTigerPhase> phase =
            new AtomicReference<>();

    public GameLifecycle getLifecycle() {
        return lifecycle.get();
    }

    public void setLifecycle(GameLifecycle lc) {
        lifecycle.set(lc);
    }

    public DragonTigerPhase getPhase() {
        return phase.get();
    }

    public void setPhase(DragonTigerPhase ph) {
        phase.set(ph);
    }

    public void clearAll() {
        players.clear();
        playerBets.clear();
        playerTokens.clear();
        phase.set(null);
        lifecycle.set(GameLifecycle.DESTROYED);
    }
}
