package com.nipunapps.cardgame.games.dragontiger.model;

import com.nipunapps.cardgame.dto.response.player.PlayerDto;
import com.nipunapps.cardgame.enums.GameLifecycle;
import com.nipunapps.cardgame.games.dragontiger.enums.DragonTigerJoinFailureCause;
import com.nipunapps.cardgame.games.dragontiger.enums.DragonTigerPhase;
import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * Response returned when a player attempts to join a Dragon Tiger game room.
 */
@Data
@Builder
public class DragonTigerJoinResult {

    /**
     * Whether the join was successful.
     */
    private boolean success;

    /**
     * If join failed, the reason for failure.
     */
    private DragonTigerJoinFailureCause failureCause;

    /**
     * Additional descriptive message for the client.
     */
    private String message;

    /**
     * Current lifecycle of the game (only populated if join is successful).
     */
    private GameLifecycle lifecycle;

    /**
     * Current phase of the game (only valid if lifecycle == RUNNING).
     */
    private DragonTigerPhase phase;

    /**
     * Number of players currently in the room (only if join is successful).
     */
    private int playerCount;

    /**
     * Snapshot of players (you may choose to hide sensitive fields).
     */
    private List<PlayerDto> players;

    public static DragonTigerJoinResult failure(DragonTigerJoinFailureCause cause, String message) {
        return DragonTigerJoinResult.builder()
                .success(false)
                .failureCause(cause)
                .message(message)
                .build();
    }
}
