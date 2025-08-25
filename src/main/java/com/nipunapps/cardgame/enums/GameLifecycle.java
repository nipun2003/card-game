package com.nipunapps.cardgame.enums;

/**
 * High-level lifecycle of a game room.
 * <p>
 * This enum indicates whether the room is inactive,
 * waiting for players, actively running, or has been destroyed.
 * </p>
 */
public enum GameLifecycle {

    /**
     * Game is not initialized or has no players.
     */
    IDLE,

    /**
     * Game room exists and is waiting for enough players to join.
     */
    WAITING,

    /**
     * Game is currently in progress (refer to {@link com.nipunapps.cardgame.games.dragontiger.enums.DragonTigerPhase}).
     */
    RUNNING,

    /**
     * Game has ended and the room is destroyed / archived.
     */
    DESTROYED;


    public static boolean isAlreadyWaitingOrRunning(GameLifecycle lifecycle) {
        return lifecycle == WAITING || lifecycle == RUNNING;
    }
}

