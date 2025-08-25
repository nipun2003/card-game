package com.nipunapps.cardgame.games.exception;

public class RoomFullException extends RuntimeException{

    public RoomFullException(String roomId) {
        super("Room with ID: " + roomId + " is full. Cannot join more players.");
    }
}
