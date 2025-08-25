package com.nipunapps.cardgame.games.dragontiger;

import com.nipunapps.cardgame.enums.GameLifecycle;
import com.nipunapps.cardgame.games.dragontiger.enums.DragonTigerJoinFailureCause;
import com.nipunapps.cardgame.models.PlayerModel;
import com.nipunapps.cardgame.sockets.WebSocketMessageSender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import reactor.test.scheduler.VirtualTimeScheduler;

import java.time.Duration;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InMemoryDragonTigerArenaTest {

    @Mock
    private WebSocketMessageSender messageSender;


    private static final String ROOM_ID = UUID.randomUUID().toString();

    private InMemoryDragonTigerArena arena;
    private VirtualTimeScheduler scheduler;

    @BeforeEach
    void setUp() {
        scheduler = VirtualTimeScheduler.getOrSet();
        arena = new InMemoryDragonTigerArena(
                messageSender, ROOM_ID, new ArenaScheduler(scheduler)
        );

        // Put some dummy token
        arena.addPlayerToken("dummy-token", "123").block();
        arena.addPlayerToken("dummy-token-2", "456").block();
        lenient().when(messageSender.sendMessage(any(), any())).thenReturn(Mono.empty());
    }

    @AfterEach
    void tearDown() {
        arena.destroyArena();
        scheduler.dispose();
    }

    @Test
    void testIfArenaIsCreatedSuccessfully() {
        assertNotNull(arena);
    }

    @Test
    @DisplayName("Test that the arena destroys after idle for max duration")
    void testIfArenaIsDestroyedSuccessfully() {
        // Fast forward virtual time
        scheduler.advanceTimeBy(Duration.ofSeconds(DragonTigerArena.MAX_IDLE_SECONDS));

        // Assert destroyed
        assertEquals(GameLifecycle.DESTROYED, arena.getLifecycle());

        // Try to join after destroyed
        final var player = PlayerModel.builder()
                .id("123")
                .name("Test Player")
                .build();

        StepVerifier.create(arena.join("dummy-token", player))
                .assertNext(result -> {
                    assertFalse(result.isSuccess());
                    assertEquals(DragonTigerJoinFailureCause.GAME_ALREADY_DESTROYED, result.getFailureCause());
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("Test that when a player joins, after max wait time game starts")
    void testIfGameStartsAfterMaxWaitTime() {
        final var player = PlayerModel.builder()
                .id("123")
                .name("Test Player")
                .build();

        StepVerifier.create(arena.join("dummy-token", player))
                .assertNext(result -> {
                    assertTrue(result.isSuccess());
                    assertEquals(1, arena.getPlayers().size());
                    assertEquals(GameLifecycle.WAITING, result.getLifecycle()); // Should be idle initially
                    verifyNoInteractions(messageSender);
                })
                .verifyComplete();
        final var player2 = PlayerModel.builder()
                .id("456")
                .name("Test Player 2")
                .build();
        scheduler.advanceTimeBy(Duration.ofSeconds(DragonTigerArena.GAME_START_DELAY_SECONDS - 3));
        StepVerifier.create(arena.join("dummy-token-2", player2))
                .assertNext(result -> {
                    assertTrue(result.isSuccess());
                    assertEquals(2, arena.getPlayers().size());
                    assertEquals(GameLifecycle.WAITING, result.getLifecycle());
                    verify(messageSender, times(1)).sendMessage(any(), any()); // Verify message sent on join
                })
                .verifyComplete();
        // Fast forward virtual time
        scheduler.advanceTimeBy(Duration.ofSeconds(3));

        // Assert game started
        assertEquals(GameLifecycle.RUNNING, arena.getLifecycle());
    }

    @Test
    @DisplayName("Test multiple players adding tokens and joining in parallel")
    void testParallelAddTokenAndJoin() {
        // Prepare players
        var playerIds = new String[]{"p1", "p2", "p3", "p4", "p5"};

        // Step 1: Add tokens in parallel
        var addTokenMonos = Flux.fromArray(playerIds)
                .flatMap(id -> arena.addPlayerToken("token-" + id, id))
                .collectList();

        StepVerifier.create(addTokenMonos)
                .assertNext(results -> assertEquals(5, results.size())) // all tokens added
                .verifyComplete();

        // Step 2: Join players in parallel
        var joinMonos = Flux.fromArray(playerIds)
                .flatMap(id -> {
                    var player = PlayerModel.builder().id(id).name("Player-" + id).build();
                    return arena.join("token-" + id, player);
                })
                .collectList();

        StepVerifier.create(joinMonos)
                .assertNext(results -> {
                    assertEquals(5, results.size());
                    results.forEach(r -> assertTrue(r.isSuccess()));
                    assertEquals(5, arena.getPlayers().size());
                    assertEquals(GameLifecycle.WAITING, arena.getLifecycle()); // still waiting before countdown finishes
                })
                .verifyComplete();

        // Fast forward scheduler to trigger game start
        scheduler.advanceTimeBy(Duration.ofSeconds(DragonTigerArena.GAME_START_DELAY_SECONDS));

        assertEquals(GameLifecycle.RUNNING, arena.getLifecycle());
    }

    @Test
    @DisplayName("Test that joining beyond max players fails with ROOM_FULL")
    void testJoinBeyondMaxPlayersFails() {
        // Prepare player IDs exceeding MAX_PLAYERS
        String[] playerIds = new String[]{"p1", "p2", "p3", "p4", "p5", "p6"};

        // Add tokens for all players
        Flux.fromArray(playerIds)
                .flatMap(id -> arena.addPlayerToken("token-" + id, id))
                .blockLast();

        // Attempt to join first MAX_PLAYERS in parallel
        Flux.fromArray(playerIds)
                .take(DragonTigerArena.MAX_PLAYERS)
                .concatMap(id -> {
                    var player = PlayerModel.builder().id(id).name("Player-" + id).build();
                    return arena.join("token-" + id, player);
                })
                .blockLast();

        // Assert arena has MAX_PLAYERS
        assertEquals(DragonTigerArena.MAX_PLAYERS, arena.getPlayers().size());

        // Attempt to join one more player (should fail)
        var extraPlayer = PlayerModel.builder().id("p6").name("Player-6").build();
        StepVerifier.create(arena.join("token-p6", extraPlayer))
                .assertNext(result -> {
                    assertFalse(result.isSuccess());
                    assertEquals(DragonTigerJoinFailureCause.ROOM_FULL, result.getFailureCause());
                })
                .verifyComplete();

        // Arena state should still be intact with MAX_PLAYERS
        assertEquals(DragonTigerArena.MAX_PLAYERS, arena.getPlayers().size());
    }


}