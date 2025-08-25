package com.nipunapps.cardgame.games.exception;

public class PlayerNotFoundException extends RuntimeException {

    public PlayerNotFoundException(String playerId, String roomId) {
        super("Player with ID: " + playerId + " not found in room with ID: " + roomId);
    }

    public PlayerNotFoundException(String playerId, String roomId, Throwable cause) {
        super("Player with ID: " + playerId + " not found in room with ID: " + roomId, cause);
    }

}
