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
     * The game is starting - world is created but players haven't been teleported yet.
     * In this state, no more players can join.
     */
    STARTING,
    
    /**
     * Players are being teleported to the game world.
     * This state ensures no game actions occur until all players are in place.
     */
    TELEPORTING,
    
    /**
     * The headstart period is active - runners can move but hunters are frozen.
     */
    HEADSTART,
    
    /**
     * The game is fully active and being played - runners and hunters are all active.
     */
    ACTIVE,
    
    /**
     * The runners have won the game.
     */
    RUNNERS_WON,
    
    /**
     * The hunters have won the game.
     */
    HUNTERS_WON,
    
    /**
     * The game has ended and cleaning up (teleporting players out, etc.).
     */
    ENDING,
    
    /**
     * The game has been fully cleaned up and world deletion is in progress.
     */
    DELETING
} 