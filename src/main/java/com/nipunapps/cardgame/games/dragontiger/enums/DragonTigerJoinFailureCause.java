package com.nipunapps.cardgame.games.dragontiger.enums;

/**
 * Possible reasons why a player failed to join a game room.
 */
public enum DragonTigerJoinFailureCause {
    INVALID_TOKEN,
    PLAYER_ALREADY_IN_ROOM,
    ROOM_FULL,
    GAME_ALREADY_DESTROYED,
    UNKNOWN_ERROR
}
