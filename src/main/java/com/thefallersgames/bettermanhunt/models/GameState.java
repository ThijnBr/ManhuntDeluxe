package com.thefallersgames.bettermanhunt.models;

/**
 * Represents the possible states of a Manhunt game.
 */
public enum GameState {
    /**
     * The game is in lobby state, accepting players and allowing team changes.
     */
    LOBBY,
    
    /**
     * The game is currently active and being played.
     */
    ACTIVE,
    
    /**
     * The game has ended (either runners won or hunters won).
     */
    GAME_ENDED
} 