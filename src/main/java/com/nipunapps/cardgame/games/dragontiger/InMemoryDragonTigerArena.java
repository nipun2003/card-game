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
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.Objects;

/**
 * Represents a Dragon Tiger game room that manages state.getPlayers(),
 * their bets, and the overall state of the game.
 * <p>
 * Responsibilities:
 * <ul>
 *   <li>Maintain the set of active state.getPlayers() within the room.</li>
 *   <li>Track player bets for the current round (cleared between rounds).</li>
 *   <li>Keep the current game lifecycle ({@link GameLifecycle}) and
 *       detailed round phase ({@link DragonTigerPhase}).</li>
 *   <li>Enforce player authentication via single-use join tokens.</li>
 * </ul>
 * <p>
 * Thread safety is ensured using concurrent collections and atomic references.
 * </p>
 */
@Slf4j
public class InMemoryDragonTigerArena implements DragonTigerArena {


    private static final int BETTING_DURATION_SECONDS = 20; // 20 seconds
    private static final int DEALING_DURATION_SECONDS = 5; // 5 seconds
    private static final int REVEALING_DURATION_SECONDS = 5; // 5 seconds


    private final DragonTigerArenaState state = new DragonTigerArenaState();
    private final ArenaScheduler scheduler;

    // Required parameters
    private final WebSocketMessageSender messageSender;
    private final String roomId;

    public InMemoryDragonTigerArena(
            WebSocketMessageSender messageSender,
            String roomId,
            ArenaScheduler scheduler
    ) {
        this.messageSender = messageSender;
        this.roomId = roomId;
        this.scheduler = scheduler;
        scheduleDestroyIfIdle();
    }


    private void scheduleDestroyIfIdle() {
        log.info("Scheduling idle destroy for room {} in {} seconds if no players join.",
                roomId, DragonTigerArena.MAX_IDLE_SECONDS);
        scheduler.scheduleIdleDestroy(
                roomId,
                state,
                () -> {
                    state.setLifecycle(GameLifecycle.DESTROYED);
                    state.setPhase(null);
                    // Additional cleanup logic
                    destroyArena();
                },
                DragonTigerArena.MAX_IDLE_SECONDS
        );
    }

    private void startGameAfterCountDown() {

        scheduler.scheduleGameStart(
                roomId,
                state,
                () -> {
                    if (state.getPlayers().isEmpty()) {
                        log.info("Room {} has no players to start the game.", roomId);
                        scheduleDestroyIfIdle();
                        return;
                    }
                    if (state.getLifecycle() == GameLifecycle.WAITING) {
                        state.setLifecycle(GameLifecycle.RUNNING);
                        state.setPhase(DragonTigerPhase.STARTING);
                        log.info("Game in room {} started.", roomId);
                    }
                },
                DragonTigerArena.GAME_START_DELAY_SECONDS,
                this::scheduleDestroyIfIdle
        );
    }

    @Override
    public Mono<Boolean> addPlayerToken(String token, String playerId) {
        return Mono.fromCallable(() -> {
            if (token == null || playerId == null) return false;
            state.getPlayerTokens().put(playerId, token);
            return true;
        });
    }

    private Mono<Void> validateJoinRequest(String token, PlayerModel player) {
        String playerId = player.getId();
        String validToken = state.getPlayerTokens().get(playerId);
        if (validToken == null || !Objects.equals(validToken, token)) {
            return Mono.error(new DragonTigerJoinException(
                    DragonTigerJoinFailureCause.INVALID_TOKEN,
                    "Invalid or expired join token."));
        }

        if (state.getPlayers().size() >= DragonTigerArena.MAX_PLAYERS) {
            return Mono.error(new DragonTigerJoinException(
                    DragonTigerJoinFailureCause.ROOM_FULL,
                    "The game room is full."));
        }

        if (state.getLifecycle() == GameLifecycle.DESTROYED) {
            return Mono.error(new DragonTigerJoinException(
                    DragonTigerJoinFailureCause.GAME_ALREADY_DESTROYED,
                    "The game has been destroyed."));
        }

        if (this.state.getPlayers().containsKey(playerId)) {
            return Mono.error(new DragonTigerJoinException(
                    DragonTigerJoinFailureCause.PLAYER_ALREADY_IN_ROOM,
                    "Player is already in the game room."));
        }

        return Mono.empty(); // means validation success
    }

    private Mono<DragonTigerJoinResult> performJoin(PlayerModel player) {
        return Mono.defer(() -> {
            PlayerModel prev = state.getPlayers().putIfAbsent(player.getId(), player);
            if (prev != null) {
                // This should not happen due to prior validation, but just in case
                return Mono.error(new DragonTigerJoinException(
                        DragonTigerJoinFailureCause.PLAYER_ALREADY_IN_ROOM,
                        "Player is already in the game room."));
            }
            initiateTheGame();
            DragonTigerJoinResult result = DragonTigerJoinResult.builder()
                    .success(true)
                    .lifecycle(state.getLifecycle())
                    .phase(state.getLifecycle() == GameLifecycle.RUNNING ? state.getPhase() : null)
                    .playerCount(this.state.getPlayers().size())
                    .players(this.state.getPlayers().values().stream().map(PlayerModel::toDto).toList())
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

        return Flux.fromIterable(state.getPlayers().values())
                .filter(p -> !p.getId().equals(player.getId())) // skip the joining player
                .flatMap(p -> messageSender.sendMessage(p.getSession(), message))
                .then(); // complete when all messages are sent
    }

    @Override
    public Mono<DragonTigerJoinResult> join(String token, PlayerModel player) {
        return validateJoinRequest(token, player)
                .then(performJoin(player))
                .doOnSuccess(t -> {
                    state.getPlayerTokens().remove(player.getId());
                    log.info("Player {} joined room {}", player.getName(), roomId);
                })
                .onErrorResume(DragonTigerJoinException.class, ex -> {
                    log.info("Player {} failed to join room {}: {} (cause: {})",
                            player.getName(), roomId, ex.getMessage(), ex.getCauseType());
                    return Mono.just(DragonTigerJoinResult.failure(ex.getCauseType(), ex.getMessage()));
                }).onErrorResume(t -> {
                    log.error("Player {} failed to join room {}: {}", player.getName(), roomId, t.getMessage(), t);
                    return Mono.just(DragonTigerJoinResult.failure(DragonTigerJoinFailureCause.UNKNOWN_ERROR, t.getMessage()));
                })
                .doFirst(() -> log.info("Player with id {} attempts to join the room {}", player.getId(), roomId));
    }

    private void initiateTheGame() {
        if (GameLifecycle.isAlreadyWaitingOrRunning(state.getLifecycle())) {
            return; // already waiting or running
        }
        state.setLifecycle(GameLifecycle.WAITING);
        scheduler.cancelAll();
        startGameAfterCountDown();
    }

    @Override
    public void destroyArena() {
        Mono.fromCallable(() -> {
            state.setLifecycle(GameLifecycle.DESTROYED);
            state.setPhase(null);
            state.getPlayers().clear();
            state.getPlayerBets().clear();
            state.getPlayerTokens().clear();
            scheduler.cancelAll();
            log.info("Room {} has been destroyed manually.", roomId);
            return true;
        });
    }

    // Package-private getter for testing purposes

    @JsonIgnore
    Map<String, PlayerModel> getPlayers() {
        return state.getPlayers();
    }

    @JsonIgnore
    Map<String, DragonTigerPlayerBet> getPlayerBets() {
        return state.getPlayerBets();
    }

    @JsonIgnore
    GameLifecycle getLifecycle() {
        return state.getLifecycle();
    }

    @JsonIgnore
    DragonTigerPhase getPhase() {
        return state.getPhase();
    }
}
