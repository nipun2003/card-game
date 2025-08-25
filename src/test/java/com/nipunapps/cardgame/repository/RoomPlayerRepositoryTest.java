package com.nipunapps.cardgame.repository;

import com.nipunapps.cardgame.models.PlayerModel;
import com.nipunapps.cardgame.repository.impl.InMemoryRoomPlayerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RoomPlayerRepositoryTest {


    private RoomPlayerRepository repository;

    private static final String ROOM_ID = "room1";

    @BeforeEach
    void setup() {
        repository = new InMemoryRoomPlayerRepository();
        // Pre-populate with some players
        repository.savePlayerToRoom(createPlayer("player1"), ROOM_ID).block();
        repository.savePlayerToRoom(createPlayer("player2"), ROOM_ID).block();
    }


    @Test
    @DisplayName("Should return empty list when no players in room")
    void testFindAllPlayersByRoomId_NoPlayers() {
        String roomId = "nonexistentRoom";
        StepVerifier.create(repository.findAllPlayersByRoomId(roomId))
                .expectNextMatches(List::isEmpty)
                .verifyComplete();
    }

    @Test
    @DisplayName("Should return all players in the room")
    void testFindAllPlayersByRoomId_WithPlayers() {
        StepVerifier.create(repository.findAllPlayersByRoomId(ROOM_ID))
                .expectNextMatches(players -> players.size() == 2)
                .verifyComplete();
    }

    @Test
    @DisplayName("Should find player by ID and room ID")
    void testFindPlayerByIdAndRoomId_Found() {
        StepVerifier.create(repository.findPlayerByIdAndRoomId("player1", ROOM_ID))
                .expectNextMatches(player -> player.getId().equals("player1"))
                .verifyComplete();
    }

    @Test
    @DisplayName("Should return empty when player not found")
    void testFindPlayerByIdAndRoomId_NotFound() {
        StepVerifier.create(repository.findPlayerByIdAndRoomId("nonexistentPlayer", ROOM_ID))
                .expectComplete()
                .verify();
    }

    @Test
    @DisplayName("Should save player to room")
    void testSavePlayerToRoom() {
        PlayerModel newPlayer = createPlayer("player3");
        StepVerifier.create(repository.savePlayerToRoom(newPlayer, ROOM_ID))
                .verifyComplete();
        StepVerifier.create(repository.findPlayerByIdAndRoomId("player3", ROOM_ID))
                .expectNextMatches(player -> player.getId().equals("player3"))
                .verifyComplete();
    }

    @Test
    @DisplayName("Should remove player from room")
    void testRemovePlayerFromRoom() {
        StepVerifier.create(repository.removePlayerFromRoom("player1", ROOM_ID))
                .verifyComplete();
        StepVerifier.create(repository.findPlayerByIdAndRoomId("player1", ROOM_ID))
                .expectComplete()
                .verify();
    }

    @Test
    @DisplayName("Should clear all players from room")
    void testClearRoom() {
        StepVerifier.create(repository.clearRoom(ROOM_ID))
                .verifyComplete();
        StepVerifier.create(repository.findAllPlayersByRoomId(ROOM_ID))
                .expectNextMatches(List::isEmpty)
                .verifyComplete();
    }

    @Test
    @DisplayName("Should update existing player in room")
    void testUpdateExistingPlayerInRoom() {
        PlayerModel updatedPlayer = PlayerModel.builder()
                .id("player1")
                .name("Updated Player 1")
                .build();
        // First, ensure player1 exists
        StepVerifier.create(repository.findPlayerByIdAndRoomId("player1", ROOM_ID))
                .assertNext(player -> {
                    assertEquals("player1", player.getId());
                    assertEquals("Player player1", player.getName());
                })
                .verifyComplete();
        StepVerifier.create(repository.savePlayerToRoom(updatedPlayer, ROOM_ID))
                .verifyComplete();
        StepVerifier.create(repository.findPlayerByIdAndRoomId("player1", ROOM_ID))
                .expectNextMatches(player -> player.getName().equals("Updated Player 1"))
                .verifyComplete();
    }

    @RepeatedTest(5)
    void testConcurrentAddsAndRemoves() throws InterruptedException {
        int threadCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        final String newRoom = "concurrentRoom";
        // Concurrently add players
        for (int i = 0; i < threadCount; i++) {
            final int idx = i;
            executor.submit(() -> {
                repository.savePlayerToRoom(
                        PlayerModel.builder()
                                .id("player-" + idx)
                                .name("Player " + idx)
                                .build(),
                        newRoom
                ).block();
                latch.countDown();
            });
        }

        latch.await(); // wait for all adds to finish

        // Verify all players added
        StepVerifier.create(repository.findAllPlayersByRoomId(newRoom))
                .expectNextMatches(players -> players.size() == threadCount)
                .verifyComplete();

        // Concurrently remove half the players while reading the full list
        CountDownLatch removeLatch = new CountDownLatch(threadCount / 2);
        for (int i = 0; i < threadCount / 2; i++) {
            final int idx = i;
            executor.submit(() -> {
                repository.removePlayerFromRoom("player-" + idx, newRoom).block();
                removeLatch.countDown();
            });
        }

        removeLatch.await(); // wait for removals

        // Read remaining players
        StepVerifier.create(repository.findAllPlayersByRoomId(newRoom))
                .expectNextMatches(players -> players.size() == threadCount - threadCount / 2)
                .verifyComplete();

        executor.shutdown();
    }

    @RepeatedTest(5)
    void testConcurrentUpdateAndRead() throws InterruptedException {
        int playerCount = 5;
        String newRoom = "concurrentRoom";
        ExecutorService executor = Executors.newFixedThreadPool(playerCount * 2);

        // Add initial players
        for (int i = 0; i < playerCount; i++) {
            repository.savePlayerToRoom(
                    PlayerModel.builder().id("p" + i).name("Player " + i).build(),
                    newRoom
            ).block();
        }

        CountDownLatch latch = new CountDownLatch(playerCount * 2);

        // Concurrently update and read
        for (int i = 0; i < playerCount; i++) {
            final int idx = i;
            // Update
            executor.submit(() -> {
                repository.savePlayerToRoom(
                        PlayerModel.builder()
                                .id("p" + idx)
                                .name("Updated Player " + idx)
                                .build(),
                        newRoom
                ).block();
                latch.countDown();
            });
            // Read
            executor.submit(() -> {
                repository.findPlayerByIdAndRoomId("p" + idx, newRoom).block();
                latch.countDown();
            });
        }

        latch.await();

        // Verify all updates
        StepVerifier.create(repository.findAllPlayersByRoomId(newRoom))
                .expectNextMatches(players -> players.stream()
                        .allMatch(p -> p.getName().startsWith("Updated Player")))
                .verifyComplete();

        executor.shutdown();
    }

    private PlayerModel createPlayer(String id) {
        return PlayerModel.builder()
                .id(id)
                .name("Player " + id)
                .build();
    }

}