package com.nipunapps.cardgame.games.exception;

public class NoTokenFoundException extends RuntimeException {

    public NoTokenFoundException(String tokenID, String roomId) {
        super("No valid room token found for tokenID: " + tokenID + " in roomId: " + roomId);
    }

    public NoTokenFoundException(String tokenId, String roomId, Throwable cause) {
        super("No valid room token found for playerId: " + tokenId + " in roomId: " + roomId, cause);
    }
}
