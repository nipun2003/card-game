package com.nipunapps.cardgame.games.dragontiger.enums;

import com.nipunapps.cardgame.enums.GameLifecycle;

/**
 * Detailed phases of a Dragon Tiger game round.
 * <p>
 * Valid only when the {@link GameLifecycle} is {@code RUNNING}.
 * </p>
 */
public enum DragonTigerPhase {

    /**
     * Countdown or setup before the round begins.
     */
    STARTING,

    /**
     * Cards are being dealt.
     */
    DEALING,

    /**
     * Players are placing bets.
     */
    BETTING,

    /**
     * Reveal the cards after betting ends.
     */
    REVEALING,

    /**
     * Result is calculated and published (bets settled).
     */
    RESULT
}
