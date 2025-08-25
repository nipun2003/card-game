package com.nipunapps.cardgame.games.dragontiger;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.nipunapps.cardgame.enums.GameLifecycle;
import com.nipunapps.cardgame.games.dragontiger.enums.DragonTigerEventType;
import com.nipunapps.cardgame.games.dragontiger.enums.DragonTigerJoinFailureCause;
import com.nipunapps.cardgame.games.dragontiger.enums.DragonTigerPhase;
import com.nipunapps.cardgame.games.dragontiger.model.DragonTigerEventMessage;
import com.nipunapps.cardgame.games.dragontiger.model.DragonTigerJoinResult;
import com.nipunapps.cardgame.games.dragontiger.model.DragonTigerPlayerBet;
import com.nipunapps.cardgame.games.dragontiger.model.PlayerJoinedMessage;
import com.nipunapps.cardgame.models.PlayerModel;
import com.nipunapps.cardgame.sockets.WebSocketMessageSender;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Represents a Dragon Tiger game room that manages players,
 * their bets, and the overall state of the game.
 * <p>
 * Responsibilities:
 * <ul>
 *   <li>Maintain the set of active players within the room.</li>
 *   <li>Track player bets for the current round (cleared between rounds).</li>
 *   <li>Keep the current game lifecycle ({@link GameLifecycle}) and
 *       detailed round phase ({@link DragonTigerPhase}).</li>
 *   <li>Enforce player authentication via single-use join tokens.</li>
 * </ul>
 * <p>
 * Thread safety is ensured using concurrent collections and atomic references.
 * </p>
 */
@RequiredArgsConstructor
public class InMemoryDragonTigerArena implements DragonTigerArena {

    /**
     * Maximum number of players allowed in this room.
     */
    private static final int MAX_PLAYERS = 5;

    /**
     * Active players in the room.
     * <p>
     * Key: playerId <br>
     * Value: {@link PlayerModel} instance representing the player.
     * </p>
     */
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
    private final Map<String, DragonTigerPlayerBet> playerBets = new ConcurrentHashMap<>();

    /**
     * High-level lifecycle of the game (e.g., WAITING, RUNNING, DESTROYED).
     */
    private final AtomicReference<GameLifecycle> lifecycle =
            new AtomicReference<>(GameLifecycle.WAITING);

    /**
     * Detailed round phase of the game (only valid when lifecycle is RUNNING).
     */
    private final AtomicReference<DragonTigerPhase> phase =
            new AtomicReference<>(DragonTigerPhase.STARTING);

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
    private final Map<String, String> playerTokens = new ConcurrentHashMap<>();


    // Required parameters
    private final WebSocketMessageSender messageSender;

    @Override
    public Mono<Boolean> addPlayerToken(String token, String playerId) {
        return Mono.defer(() -> {
            if (token == null || playerId == null) {
                return Mono.just(false);
            }
            playerTokens.put(playerId, token);
            return Mono.just(true);
        });
    }

    @Override
    public Mono<DragonTigerJoinResult> join(String token, PlayerModel player) {
        return Mono.defer(() -> {

            String playerId = player.getId();
            String validToken = playerTokens.get(playerId);

            if (validToken == null || !Objects.equals(validToken, token)) {
                return Mono.just(DragonTigerJoinResult.failure(DragonTigerJoinFailureCause.INVALID_TOKEN,
                        "Invalid or expired join token."));
            }

            // Invalidate immediately
            playerTokens.remove(playerId);

            if (players.size() >= MAX_PLAYERS) {
                return Mono.just(DragonTigerJoinResult.failure(DragonTigerJoinFailureCause.ROOM_FULL,
                        "The game room is full."));
            }

            if (lifecycle.get() == GameLifecycle.DESTROYED) {
                return Mono.just(DragonTigerJoinResult.failure(DragonTigerJoinFailureCause.GAME_ALREADY_DESTROYED,
                        "The game has been destroyed."));
            }

            if (players.containsKey(playerId)) {
                return Mono.just(DragonTigerJoinResult.failure(DragonTigerJoinFailureCause.PLAYER_ALREADY_IN_ROOM,
                        "Player is already in the game room."));
            }

            players.put(playerId, player);

            return broadCastPlayerJoined(player)
                    .then(Mono.just(DragonTigerJoinResult.builder()
                            .success(true)
                            .lifecycle(lifecycle.get())
                            .phase(lifecycle.get() == GameLifecycle.RUNNING ? phase.get() : null)
                            .playerCount(players.size())
                            .players(players.values().stream().map(PlayerModel::toDto).toList())
                            .build()));
        });
    }

    private Mono<Void> broadCastPlayerJoined(PlayerModel player) {
        DragonTigerEventMessage<PlayerJoinedMessage> message =
                DragonTigerEventMessage.<PlayerJoinedMessage>builder()
                        .eventType(DragonTigerEventType.PLAYER_JOINED)
                        .data(PlayerJoinedMessage.builder()
                                .id(player.getId())
                                .name(player.getName())
                                .build())
                        .build();

        return Flux.fromIterable(players.values())
                .filter(p -> !p.getId().equals(player.getId())) // skip the joining player
                .flatMap(p -> messageSender.sendMessage(p.getSession(), message))
                .then(); // complete when all messages are sent
    }


    // Package-private getter for testing purposes

    @JsonIgnore
    Map<String, PlayerModel> getPlayers() {
        return players;
    }

    @JsonIgnore
    Map<String, DragonTigerPlayerBet> getPlayerBets() {
        return playerBets;
    }

    @JsonIgnore
    GameLifecycle getLifecycle() {
        return lifecycle.get();
    }

    @JsonIgnore
    DragonTigerPhase getPhase() {
        return phase.get();
    }
}
