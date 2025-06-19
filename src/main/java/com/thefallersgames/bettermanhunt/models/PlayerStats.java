package com.thefallersgames.bettermanhunt.models;

import java.util.UUID;

/**
 * Stores statistics for a player in Manhunt games.
 */
public class PlayerStats {
    private final UUID playerId;
    private String playerName;
    private int gamesPlayed;
    private int runnerWins;
    private int hunterWins;
    private int kills;
    private int deaths;
    private int dragonKills;
    
    /**
     * Creates a new player stats object for a specific player.
     *
     * @param playerId The UUID of the player
     * @param playerName The name of the player
     */
    public PlayerStats(UUID playerId, String playerName) {
        this.playerId = playerId;
        this.playerName = playerName;
        this.gamesPlayed = 0;
        this.runnerWins = 0;
        this.hunterWins = 0;
        this.kills = 0;
        this.deaths = 0;
        this.dragonKills = 0;
    }
    
    // Getters
    
    public UUID getPlayerId() {
        return playerId;
    }
    
    public String getPlayerName() {
        return playerName;
    }
    
    public int getGamesPlayed() {
        return gamesPlayed;
    }
    
    public int getRunnerWins() {
        return runnerWins;
    }
    
    public int getHunterWins() {
        return hunterWins;
    }
    
    public int getKills() {
        return kills;
    }
    
    public int getDeaths() {
        return deaths;
    }
    
    public int getDragonKills() {
        return dragonKills;
    }
    
    // Updaters
    
    public void setPlayerName(String playerName) {
        this.playerName = playerName;
    }
    
    public void incrementGamesPlayed() {
        this.gamesPlayed++;
    }
    
    public void incrementRunnerWins() {
        this.runnerWins++;
    }
    
    public void incrementHunterWins() {
        this.hunterWins++;
    }
    
    public void incrementKills() {
        this.kills++;
    }
    
    public void incrementDeaths() {
        this.deaths++;
    }
    
    public void incrementDragonKills() {
        this.dragonKills++;
    }
} 