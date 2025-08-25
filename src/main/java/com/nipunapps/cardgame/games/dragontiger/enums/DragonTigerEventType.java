package com.nipunapps.cardgame.games.dragontiger.enums;

public enum DragonTigerEventType {

    // Game lifecycle
    GAME_CREATED,
    GAME_STARTED,
    GAME_ENDED,
    GAME_DESTROYED,

    // Player lifecycle
    PLAYER_JOINED,
    PLAYER_LEFT,
    PLAYER_KICKED,

    // Betting
    BET_OPENED,
    BET_PLACED,
    BET_CLOSED,
    BET_CANCELLED,

    // Round flow
    ROUND_STARTED,
    CARDS_DEALT,
    ROUND_RESULT,
    ROUND_ENDED,

    // Errors / system
    INVALID_ACTION,
    SERVER_ERROR;
}
