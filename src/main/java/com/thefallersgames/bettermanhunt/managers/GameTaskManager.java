package com.thefallersgames.bettermanhunt.managers;

import com.thefallersgames.bettermanhunt.Plugin;
import com.thefallersgames.bettermanhunt.models.Game;
import com.thefallersgames.bettermanhunt.tasks.CompassTask;
import com.thefallersgames.bettermanhunt.tasks.HeadstartTask;
import org.bukkit.boss.BossBar;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Manages game-related tasks such as compass tracking and headstart timers.
 */
public class GameTaskManager {
    private final Plugin plugin;
    private final Supplier<GameManager> gameManagerSupplier;
    private final Map<String, CompassTask> compassTasks;
    private final Map<String, HeadstartTask> headstartTasks;
    
    /**
     * Creates a new GameTaskManager.
     * 
     * @param plugin The plugin instance
     * @param gameManagerSupplier A supplier for the game manager instance
     * @param bossBarManager The boss bar manager
     */
    public GameTaskManager(Plugin plugin, Supplier<GameManager> gameManagerSupplier, BossBarManager bossBarManager) {
        this.plugin = plugin;
        this.gameManagerSupplier = gameManagerSupplier;
        this.compassTasks = new HashMap<>();
        this.headstartTasks = new HashMap<>();
    }
    
    /**
     * Gets the game manager from the supplier.
     * 
     * @return The game manager
     */
    private GameManager getGameManager() {
        return gameManagerSupplier.get();
    }
    
    /**
     * Creates and starts a headstart task.
     * 
     * @param game The game to start the headstart for
     * @param bossBar The boss bar to use for the headstart timer
     */
    public void startHeadstartTask(Game game, BossBar bossBar) {
        String gameName = game.getName();
        
        // Start headstart countdown
        HeadstartTask headstartTask = new HeadstartTask(plugin, game, bossBar, getGameManager(), plugin.getGameTaskService());
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
    }
} 