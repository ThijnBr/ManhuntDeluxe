package com.thefallersgames.bettermanhunt.managers;

import com.thefallersgames.bettermanhunt.models.Game;
import org.bukkit.Bukkit;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Manages boss bars for Manhunt games.
 */
public class BossBarManager {
    private final Map<String, BossBar> gameBossBars;
    
    /**
     * Creates a new BossBarManager.
     */
    public BossBarManager() {
        this.gameBossBars = new HashMap<>();
    }
    
    /**
     * Creates a boss bar for a game in lobby state.
     * 
     * @param game The game to create a boss bar for
     */
    public void createLobbyBossBar(Game game) {
        String gameName = game.getName();
        
        // Check if there's already a boss bar for this game
        if (gameBossBars.containsKey(gameName)) {
            return;
        }
        
        // Create boss bar for lobby
        BossBar bossBar = Bukkit.createBossBar(
                "Waiting for players to join...",
                BarColor.BLUE,
                BarStyle.SOLID
        );
        bossBar.setProgress(1.0); // Full bar
        gameBossBars.put(gameName, bossBar);
        
        // Add all current players to the boss bar
        for (UUID playerId : game.getAllPlayers()) {
            Player player = Bukkit.getPlayer(playerId);
            if (player != null) {
                bossBar.addPlayer(player);
            }
        }
    }
    
    /**
     * Creates a headstart boss bar.
     * 
     * @param gameName The name of the game
     * @return The created boss bar
     */
    public BossBar createHeadstartBossBar(String gameName) {
        // Create boss bar for headstart countdown
        BossBar bossBar = Bukkit.createBossBar(
                "Headstart Time Remaining",
                BarColor.GREEN,
                BarStyle.SOLID
        );
        gameBossBars.put(gameName, bossBar);
        
        return bossBar;
    }
    
    /**
     * Creates an active game boss bar showing the number of runners.
     * 
     * @param game The game
     * @return The created boss bar
     */
    public BossBar createActiveGameBossBar(Game game) {
        String gameName = game.getName();
        int totalRunners = game.getRunners().size();
        
        // Create boss bar for active game
        BossBar bossBar = Bukkit.createBossBar(
                "Runners Remaining: " + totalRunners + "/" + totalRunners,
                BarColor.YELLOW,
                BarStyle.SOLID
        );
        bossBar.setProgress(1.0); // Full bar initially
        gameBossBars.put(gameName, bossBar);
        
        // Add all current players to the boss bar
        for (UUID playerId : game.getAllPlayers()) {
            Player player = Bukkit.getPlayer(playerId);
            if (player != null) {
                bossBar.addPlayer(player);
            }
        }
        
        return bossBar;
    }
    
    /**
     * Updates the active game boss bar with current runner count.
     * 
     * @param game The game to update boss bar for
     */
    public void updateActiveGameBossBar(Game game) {
        BossBar bossBar = gameBossBars.get(game.getName());
        if (bossBar == null) {
            return;
        }
        
        int remainingRunners = 0;
        int totalRunners = game.getRunners().size();
        
        // Count alive runners
        for (UUID runnerId : game.getRunners()) {
            Player runner = Bukkit.getPlayer(runnerId);
            if (runner != null && runner.isOnline() && !runner.isDead()) {
                remainingRunners++;
            }
        }
        
        // Update title and progress
        bossBar.setTitle("Runners Remaining: " + remainingRunners + "/" + totalRunners);
        
        // Update progress bar (1.0 = full, 0.0 = empty)
        double progress = totalRunners > 0 ? (double) remainingRunners / totalRunners : 0.0;
        bossBar.setProgress(Math.max(0.0, Math.min(1.0, progress)));
        
        // Change color based on remaining runners
        if (remainingRunners == 0) {
            bossBar.setColor(BarColor.RED);
        } else if (remainingRunners < totalRunners / 2) {
            bossBar.setColor(BarColor.YELLOW);
        } else {
            bossBar.setColor(BarColor.GREEN);
        }
    }
    
    /**
     * Updates the lobby boss bar with current game information.
     * 
     * @param game The game to update boss bar for
     */
    public void updateLobbyBossBar(Game game) {
        BossBar bossBar = gameBossBars.get(game.getName());
        if (bossBar == null) {
            return;
        }
        
        int hunters = game.getHunters().size();
        int runners = game.getRunners().size();
        
        bossBar.setTitle("Lobby: " + hunters + " Hunter(s), " + runners + " Runner(s)");
    }
    
    /**
     * Updates the boss bar for game end.
     * 
     * @param gameName The name of the game
     * @param runnersWon Whether the runners won
     */
    public void updateGameEndBossBar(String gameName, boolean runnersWon) {
        BossBar bossBar = gameBossBars.get(gameName);
        if (bossBar != null) {
            if (runnersWon) {
                bossBar.setTitle("Game Over - Runners Won!");
                bossBar.setColor(BarColor.GREEN);
            } else {
                bossBar.setTitle("Game Over - Hunters Won!");
                bossBar.setColor(BarColor.RED);
            }
        }
    }
    
    /**
     * Adds a player to a game's boss bar.
     * 
     * @param gameName The name of the game
     * @param player The player to add
     */
    public void addPlayerToBossBar(String gameName, Player player) {
        BossBar bossBar = gameBossBars.get(gameName);
        if (bossBar != null) {
            bossBar.addPlayer(player);
        }
    }
    
    /**
     * Removes a player from a game's boss bar.
     * 
     * @param gameName The name of the game
     * @param player The player to remove
     */
    public void removePlayerFromBossBar(String gameName, Player player) {
        BossBar bossBar = gameBossBars.get(gameName);
        if (bossBar != null) {
            bossBar.removePlayer(player);
        }
    }
    
    /**
     * Removes a boss bar completely.
     * 
     * @param gameName The name of the game
     */
    public void removeBossBar(String gameName) {
        BossBar bossBar = gameBossBars.remove(gameName);
        if (bossBar != null) {
            bossBar.removeAll();
        }
    }
    
    /**
     * Gets a boss bar by game name.
     * 
     * @param gameName The name of the game
     * @return The boss bar, or null if none exists
     */
    public BossBar getBossBar(String gameName) {
        return gameBossBars.get(gameName);
    }
} 