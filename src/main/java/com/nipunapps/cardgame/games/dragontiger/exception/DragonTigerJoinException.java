package com.nipunapps.cardgame.games.dragontiger.exception;

import com.nipunapps.cardgame.games.dragontiger.enums.DragonTigerJoinFailureCause;

public class DragonTigerJoinException extends RuntimeException {
    private final DragonTigerJoinFailureCause causeType;

    public DragonTigerJoinException(DragonTigerJoinFailureCause causeType, String message) {
        super(message);
        this.causeType = causeType;
    }

    public DragonTigerJoinFailureCause getCauseType() {
        return causeType;
    }
}
