package com.thefallersgames.bettermanhunt.services;

import com.thefallersgames.bettermanhunt.Plugin;
import com.thefallersgames.bettermanhunt.managers.GameManager;
import com.thefallersgames.bettermanhunt.managers.GameSetupManager;
import com.thefallersgames.bettermanhunt.managers.HeadstartManager;
import com.thefallersgames.bettermanhunt.models.Game;
import com.thefallersgames.bettermanhunt.tasks.CompassTask;
import com.thefallersgames.bettermanhunt.tasks.HeadstartTask;
import org.bukkit.Bukkit;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Service responsible for managing all game-related tasks, including boss bars
 * and scheduled tasks like compass tracking and headstart timers.
 */
public class GameTaskService {
    private final Plugin plugin;
    private final Supplier<GameManager> gameManagerSupplier;
    private GameManager gameManager; // Lazily initialized
    private final GameSetupManager gameSetupManager;
    
    // Task tracking
    private final Map<String, CompassTask> compassTasks = new HashMap<>();
    private final Map<String, HeadstartTask> headstartTasks = new HashMap<>();
    
    // Boss bar tracking
    private final Map<String, BossBar> gameBossBars = new HashMap<>();
    
    /**
     * Creates a new GameTaskService.
     * 
     * @param plugin The plugin instance
     * @param gameManagerSupplier A supplier for the game manager to avoid circular dependency
     * @param headstartManager The headstart manager to use
     */
    public GameTaskService(
            Plugin plugin,
            Supplier<GameManager> gameManagerSupplier,
            HeadstartManager headstartManager) {
        this.plugin = plugin;
        this.gameManagerSupplier = gameManagerSupplier;
        this.gameSetupManager = new GameSetupManager(headstartManager);
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
        removeBossBar(gameName);
        
        // Create boss bar for headstart countdown
        BossBar bossBar = createHeadstartBossBar(gameName);
        
        // Set up players
        gameSetupManager.setupPlayersForGame(game, bossBar, playerSetupCallback);
        
        // Start tasks
        startHeadstartTask(game, bossBar);
        startCompassTask(game);
    }
    
    // ---------------------- Boss Bar Methods ----------------------
    
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
    
    // ---------------------- Task Methods ----------------------
    
    /**
     * Creates and starts a headstart task.
     * 
     * @param game The game to start the headstart for
     * @param bossBar The boss bar to use for the headstart timer
     */
    public void startHeadstartTask(Game game, BossBar bossBar) {
        String gameName = game.getName();
        
        // Start headstart countdown
        HeadstartTask headstartTask = new HeadstartTask(plugin, game, bossBar, getGameManager(), this);
        headstartTask.runTaskTimer(plugin, 0L, 20L); // Update every second
        headstartTasks.put(gameName, headstartTask);
    }
    
    /**
     * Creates and starts a compass task.
     * 
     * @param game The game to start the compass task for
     */
    public void startCompassTask(Game game) {
        String gameName = game.getName();
        
        // Create compass task
        CompassTask compassTask = new CompassTask(plugin, game);
        compassTasks.put(gameName, compassTask);
    }
    
    /**
     * Gets the compass task for a game.
     * 
     * @param gameName The name of the game
     * @return The compass task, or null if none exists
     */
    public CompassTask getCompassTask(String gameName) {
        return compassTasks.get(gameName);
    }
    
    /**
     * Cancels a headstart task.
     * 
     * @param gameName The name of the game
     */
    public void cancelHeadstartTask(String gameName) {
        HeadstartTask headstartTask = headstartTasks.remove(gameName);
        if (headstartTask != null) {
            headstartTask.cancel();
        }
    }
    
    /**
     * Cancels a compass task.
     * 
     * @param gameName The name of the game
     */
    public void cancelCompassTask(String gameName) {
        compassTasks.remove(gameName);
    }
    
    /**
     * Cleans up all tasks associated with a game.
     * 
     * @param gameName The name of the game
     */
    public void cleanupGameTasks(String gameName) {
        // Cancel compass task
        cancelCompassTask(gameName);
        
        // Cancel headstart task
        cancelHeadstartTask(gameName);
        
        // Remove boss bar
        removeBossBar(gameName);
    }
    
    /**
     * Handles the end of a game, updating the boss bar.
     * 
     * @param game The game that ended
     * @param runnersWon Whether the runners won
     */
    public void handleGameEnd(Game game, boolean runnersWon) {
        // Update boss bar
        updateGameEndBossBar(game.getName(), runnersWon);
        
        // Cancel headstart task if still running
        cancelHeadstartTask(game.getName());
    }
    
    /**
     * Gets the game setup manager.
     *
     * @return The game setup manager
     */
    public GameSetupManager getGameSetupManager() {
        return gameSetupManager;
    }
} 