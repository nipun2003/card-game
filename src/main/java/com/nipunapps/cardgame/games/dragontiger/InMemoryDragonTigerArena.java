package com.nipunapps.cardgame.games.dragontiger;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.nipunapps.cardgame.enums.GameLifecycle;
import com.nipunapps.cardgame.games.dragontiger.enums.DragonTigerEventType;
import com.nipunapps.cardgame.games.dragontiger.enums.DragonTigerJoinFailureCause;
import com.nipunapps.cardgame.games.dragontiger.enums.DragonTigerPhase;
import com.nipunapps.cardgame.games.dragontiger.exception.DragonTigerJoinException;
import com.nipunapps.cardgame.games.dragontiger.model.DragonTigerEventMessage;
import com.nipunapps.cardgame.games.dragontiger.model.DragonTigerJoinResult;
import com.nipunapps.cardgame.games.dragontiger.model.DragonTigerPlayerBet;
import com.nipunapps.cardgame.games.dragontiger.model.PlayerJoinedMessage;
import com.nipunapps.cardgame.models.PlayerModel;
import com.nipunapps.cardgame.sockets.WebSocketMessageSender;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
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
@Slf4j
public class InMemoryDragonTigerArena implements DragonTigerArena {

    /**
     * Maximum number of players allowed in this room.
     */
    private static final int MAX_PLAYERS = 5;

    private static final int MAX_IDLE_SECONDS = 90; // 1.5 minutes
    private static final int GAME_START_DELAY_SECONDS = 5; // 15 seconds
    private static final int BETTING_DURATION_SECONDS = 20; // 20 seconds
    private static final int DEALING_DURATION_SECONDS = 5; // 5 seconds
    private static final int REVEALING_DURATION_SECONDS = 5; // 5 seconds

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
            new AtomicReference<>(GameLifecycle.IDLE);

    /**
     * Detailed round phase of the game (only valid when lifecycle is RUNNING).
     */
    private final AtomicReference<DragonTigerPhase> phase =
            new AtomicReference<>();

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

    private final AtomicReference<Disposable> idleDestroyTask = new AtomicReference<>();
    private final AtomicReference<Disposable> gameStartTask = new AtomicReference<>();

    // Required parameters
    private final WebSocketMessageSender messageSender;
    private final String roomId;

    private void scheduleDestroyIfIdle() {
        cancelTask(idleDestroyTask);

        Disposable disposable = Mono.delay(Duration.ofSeconds(MAX_IDLE_SECONDS))
                .filter(t -> players.isEmpty() && lifecycle.get() == GameLifecycle.IDLE) // 👈 prevent destroy in running phase
                .doOnNext(t -> log.info("Room {} has been idle for {} seconds. Destroying...",
                        roomId, MAX_IDLE_SECONDS))
                .doOnNext(t -> {
                    lifecycle.set(GameLifecycle.DESTROYED);
                    phase.set(null);
                    // Additional cleanup logic
                })
                .doOnNext(t -> log.info("Room {} destroyed due to inactivity.", roomId))
                .subscribe();

        idleDestroyTask.set(disposable);
    }

    private void startGameAfterCountDown() {
        cancelTask(gameStartTask);

        Disposable disposable = Mono.delay(Duration.ofSeconds(GAME_START_DELAY_SECONDS))
                .doOnNext(t -> {
                    if (players.isEmpty()) {
                        log.info("Room {} still has no players after {}s, rescheduling idle destroy...",
                                roomId, GAME_START_DELAY_SECONDS);
                        scheduleDestroyIfIdle();
                        return;
                    }

                    if (lifecycle.get() == GameLifecycle.WAITING) {
                        lifecycle.set(GameLifecycle.RUNNING);
                        phase.set(DragonTigerPhase.STARTING);
                        log.info("Game in room {} started.", roomId);
                    }
                })
                .subscribe();

        gameStartTask.set(disposable);
    }

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

    private Mono<Void> validateJoinRequest(String token, PlayerModel player) {
        String playerId = player.getId();
        String validToken = playerTokens.get(playerId);

        if (validToken == null || !Objects.equals(validToken, token)) {
            return Mono.error(new DragonTigerJoinException(
                    DragonTigerJoinFailureCause.INVALID_TOKEN,
                    "Invalid or expired join token."));
        }

        if (players.size() >= MAX_PLAYERS) {
            return Mono.error(new DragonTigerJoinException(
                    DragonTigerJoinFailureCause.ROOM_FULL,
                    "The game room is full."));
        }

        if (lifecycle.get() == GameLifecycle.DESTROYED) {
            return Mono.error(new DragonTigerJoinException(
                    DragonTigerJoinFailureCause.GAME_ALREADY_DESTROYED,
                    "The game has been destroyed."));
        }

        if (players.containsKey(playerId)) {
            return Mono.error(new DragonTigerJoinException(
                    DragonTigerJoinFailureCause.PLAYER_ALREADY_IN_ROOM,
                    "Player is already in the game room."));
        }

        return Mono.empty(); // means validation success
    }

    private Mono<DragonTigerJoinResult> performJoin(PlayerModel player) {
        return Mono.defer(() -> {
            PlayerModel prev = players.putIfAbsent(player.getId(), player);
            if (prev != null) {
                // This should not happen due to prior validation, but just in case
                return Mono.error(new DragonTigerJoinException(
                        DragonTigerJoinFailureCause.PLAYER_ALREADY_IN_ROOM,
                        "Player is already in the game room."));
            }
            DragonTigerJoinResult result = DragonTigerJoinResult.builder()
                    .success(true)
                    .lifecycle(lifecycle.get())
                    .phase(lifecycle.get() == GameLifecycle.RUNNING ? phase.get() : null)
                    .playerCount(players.size())
                    .players(players.values().stream().map(PlayerModel::toDto).toList())
                    .build();

            return broadCastPlayerJoined(player).thenReturn(result);
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

    @Override
    public Mono<DragonTigerJoinResult> join(String token, PlayerModel player) {
        return validateJoinRequest(token, player)
                .then(performJoin(player))
                .doOnSuccess(t -> {
                    playerTokens.remove(player.getId());
                    log.info("Player {} joined room {}", player.getName(), roomId);
                    waitForGameStart();
                })
                .onErrorResume(DragonTigerJoinException.class, ex -> {
                    log.info("Player {} failed to join room {}: {} (cause: {})",
                            player.getName(), roomId, ex.getMessage(), ex.getCauseType());
                    if (players.isEmpty()) scheduleDestroyIfIdle();
                    return Mono.just(DragonTigerJoinResult.failure(ex.getCauseType(), ex.getMessage()));
                }).onErrorResume(t -> {
                    log.error("Player {} failed to join room {}: {}", player.getName(), roomId, t.getMessage());
                    if (players.isEmpty()) scheduleDestroyIfIdle();
                    return Mono.just(DragonTigerJoinResult.failure(DragonTigerJoinFailureCause.UNKNOWN_ERROR, t.getMessage()));
                })
                .doFirst(() -> log.info("Player with id {} attempts to join the room {}", player.getId(), roomId));
    }

    private void waitForGameStart() {
        if (GameLifecycle.isAlreadyWaitingOrRunning(lifecycle.get())) {
            return; // already waiting or running
        }
        lifecycle.set(GameLifecycle.WAITING);
        cancelTask(idleDestroyTask);
        startGameAfterCountDown();
    }

    // Room helper methods
    private void cancelTask(AtomicReference<Disposable> ref) {
        Disposable d = ref.getAndSet(null);
        if (d != null && !d.isDisposed()) {
            d.dispose();
        }
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
