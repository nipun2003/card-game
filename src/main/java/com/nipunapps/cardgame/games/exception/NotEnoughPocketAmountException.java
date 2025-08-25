package com.nipunapps.cardgame.games.exception;

public class NotEnoughPocketAmountException extends RuntimeException{

    public NotEnoughPocketAmountException(String playerId, long requiredAmount, long currentAmount) {
        super("Player with ID: " + playerId + " does not have enough pocket amount. Required: " + requiredAmount + ", Current: " + currentAmount);
    }
}
