package com.thefallersgames.bettermanhunt.managers;

import com.thefallersgames.bettermanhunt.Plugin;
import com.thefallersgames.bettermanhunt.models.Game;
import com.thefallersgames.bettermanhunt.tasks.CompassTask;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;

import java.util.function.Supplier;

/**
 * Coordinates task management across specialized managers.
 * Acts as a facade for BossBarManager, GameTaskManager, and GameSetupManager.
 */
public class TaskManager {
    private final Plugin plugin;
    private final Supplier<GameManager> gameManagerSupplier;
    private final BossBarManager bossBarManager;
    private final GameTaskManager gameTaskManager;
    private final GameSetupManager gameSetupManager;
    private GameManager gameManager; // Lazily initialized
    
    /**
     * Creates a new TaskManager.
     * 
     * @param plugin The plugin instance
     * @param gameManagerSupplier A supplier for the game manager to avoid circular dependency
     */
    public TaskManager(Plugin plugin, Supplier<GameManager> gameManagerSupplier) {
        this.plugin = plugin;
        this.gameManagerSupplier = gameManagerSupplier;
        this.bossBarManager = new BossBarManager();
        
        // Create headstart manager and setup manager
        HeadstartManager headstartManager = new HeadstartManager(plugin);
        this.gameSetupManager = new GameSetupManager(headstartManager);
        
        // Initialize game task manager
        this.gameTaskManager = new GameTaskManager(plugin, () -> getGameManager(), bossBarManager);
    }
    
    /**
     * Gets the game manager, initializing it lazily.
     * 
     * @return The game manager
     */
    private GameManager getGameManager() {
        if (gameManager == null) {
            gameManager = gameManagerSupplier.get();
        }
        return gameManager;
    }
    
    /**
     * Sets up a game start with boss bar, headstart timer, and compass tasks.
     * 
     * @param game The game to set up
     * @param playerSetupCallback Callback to set up each player
     */
    public void setupGameStart(Game game, GameSetupManager.PlayerSetupCallback playerSetupCallback) {
        String gameName = game.getName();
        
        // Remove the lobby boss bar since the game is now starting
        bossBarManager.removeBossBar(gameName);
        
        // Create boss bar for headstart countdown
        BossBar bossBar = bossBarManager.createHeadstartBossBar(gameName);
        
        // Set up players
        gameSetupManager.setupPlayersForGame(game, bossBar, playerSetupCallback);
        
        // Start tasks
        gameTaskManager.startHeadstartTask(game, bossBar);
        gameTaskManager.startCompassTask(game);
    }
    
    /**
     * Updates the active game boss bar with current runner count.
     * 
     * @param game The game to update boss bar for
     */
    public void updateActiveGameBossBar(Game game) {
        bossBarManager.updateActiveGameBossBar(game);
    }
    
    /**
     * Handles the end of a game, updating the boss bar.
     * 
     * @param game The game that ended
     * @param runnersWon Whether the runners won
     */
    public void handleGameEnd(Game game, boolean runnersWon) {
        // Update boss bar
        bossBarManager.updateGameEndBossBar(game.getName(), runnersWon);
        
        // Cancel headstart task if still running
        gameTaskManager.cancelHeadstartTask(game.getName());
    }
    
    /**
     * Creates a boss bar for a game in lobby state.
     * 
     * @param game The game to create a boss bar for
     */
    public void createLobbyBossBar(Game game) {
        bossBarManager.createLobbyBossBar(game);
    }
    
    /**
     * Updates the lobby boss bar with current game information.
     * 
     * @param game The game to update boss bar for
     */
    public void updateLobbyBossBar(Game game) {
        bossBarManager.updateLobbyBossBar(game);
    }
    
    /**
     * Adds a player to a game's boss bar.
     * 
     * @param gameName The name of the game
     * @param player The player to add
     */
    public void addPlayerToBossBar(String gameName, Player player) {
        bossBarManager.addPlayerToBossBar(gameName, player);
    }
    
    /**
     * Removes a player from a game's boss bar.
     * 
     * @param gameName The name of the game
     * @param player The player to remove
     */
    public void removePlayerFromBossBar(String gameName, Player player) {
        bossBarManager.removePlayerFromBossBar(gameName, player);
    }
    
    /**
     * Removes a boss bar completely.
     * 
     * @param gameName The name of the game
     */
    public void removeBossBar(String gameName) {
        bossBarManager.removeBossBar(gameName);
    }
    
    /**
     * Gets the compass task for a game.
     * 
     * @param gameName The name of the game
     * @return The compass task, or null if none exists
     */
    public CompassTask getCompassTask(String gameName) {
        return gameTaskManager.getCompassTask(gameName);
    }
    
    /**
     * Cleans up all tasks associated with a game.
     * 
     * @param gameName The name of the game
     */
    public void cleanupGameTasks(String gameName) {
        // Clean up game tasks
        gameTaskManager.cleanupGameTasks(gameName);
        
        // Remove boss bar
        bossBarManager.removeBossBar(gameName);
    }
} 