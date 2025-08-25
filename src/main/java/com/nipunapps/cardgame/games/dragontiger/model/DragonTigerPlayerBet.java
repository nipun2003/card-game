package com.nipunapps.cardgame.games.dragontiger.model;

import com.nipunapps.cardgame.games.dragontiger.enums.DragonTigerBetOption;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents a bet placed by a player in the Dragon Tiger game.
 * <p>
 * Each bet contains the player's unique identifier, the bet amount,
 * and the option chosen (Dragon, Tiger, or Tie).
 * </p>
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class DragonTigerPlayerBet {

    /**
     * Unique identifier of the player placing the bet.
     */
    private String playerId;

    /**
     * The amount wagered by the player.
     */
    private int amount;

    /**
     * The option selected by the player to bet on.
     * Can be Dragon, Tiger, or Tie.
     */
    private DragonTigerBetOption betOn;
}
