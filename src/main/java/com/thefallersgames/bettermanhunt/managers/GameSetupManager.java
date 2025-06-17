package com.thefallersgames.bettermanhunt.managers;

import com.thefallersgames.bettermanhunt.models.Game;
import org.bukkit.Bukkit;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;

import java.util.UUID;

/**
 * Manages the setup of players and resources for game start.
 */
public class GameSetupManager {
    private final HeadstartManager headstartManager;
    
    /**
     * Creates a new GameSetupManager.
     * 
     * @param headstartManager The headstart manager to use
     */
    public GameSetupManager(HeadstartManager headstartManager) {
        this.headstartManager = headstartManager;
    }
    
    /**
     * Sets up players for a game.
     * 
     * @param game The game to set up players for
     * @param bossBar The boss bar to add players to
     * @param playerSetupCallback The callback to set up each player
     */
    public void setupPlayersForGame(Game game, BossBar bossBar, PlayerSetupCallback playerSetupCallback) {
        // Set up hunters and runners
        for (UUID playerId : game.getHunters()) {
            Player player = Bukkit.getPlayer(playerId);
            if (player != null) {
                bossBar.addPlayer(player);
                playerSetupCallback.setup(game, player, true); // Hunter
            }
        }
        
        for (UUID playerId : game.getRunners()) {
            Player player = Bukkit.getPlayer(playerId);
            if (player != null) {
                bossBar.addPlayer(player);
                playerSetupCallback.setup(game, player, false); // Runner
            }
        }
    }
    
    /**
     * Sets up a player for game start.
     * 
     * @param game The game
     * @param player The player to set up
     * @param isHunter Whether the player is a hunter
     */
    public void setupPlayer(Game game, Player player, boolean isHunter) {
        // Clear inventory to remove lobby items
        player.getInventory().clear();
        
        // Teleport to world spawn location instead of game spawn location
        boolean teleportSuccess = false;
        try {
            teleportSuccess = player.teleport(game.getWorld().getSpawnLocation());
            
            if (!teleportSuccess) {
                player.sendMessage(org.bukkit.ChatColor.RED + "Failed to teleport to game world. You may experience issues.");
            }
        } catch (Exception e) {
            player.sendMessage(org.bukkit.ChatColor.RED + "Error teleporting to game world: " + e.getMessage());
        }
        
        if (isHunter) {
            headstartManager.freezeHunter(player);
        } else {
            player.setGameMode(org.bukkit.GameMode.SURVIVAL);
        }
    }
    
    /**
     * Callback interface for setting up players at game start.
     */
    @FunctionalInterface
    public interface PlayerSetupCallback {
        /**
         * Sets up a player for the game.
         * 
         * @param game The game
         * @param player The player
         * @param isHunter Whether the player is a hunter
         */
        void setup(Game game, Player player, boolean isHunter);
    }
} 