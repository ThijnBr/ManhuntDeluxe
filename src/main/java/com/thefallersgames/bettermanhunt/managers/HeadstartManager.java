package com.thefallersgames.bettermanhunt.managers;

import com.thefallersgames.bettermanhunt.models.Game;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages the headstart period for hunters, keeping them frozen at spawn.
 */
public class HeadstartManager {
    private final Map<UUID, Location> frozenHunterLocations = new ConcurrentHashMap<>();
    private final Plugin plugin;
    private final Map<String, Integer> headstartTaskIds = new ConcurrentHashMap<>();

    /**
     * Constructs a new HeadstartManager.
     *
     * @param plugin The plugin instance
     */
    public HeadstartManager(Plugin plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Starts the headstart period for a game.
     * 
     * @param game The game to start headstart for
     * @param completionCallback Callback to run when headstart completes
     */
    public void startHeadstart(Game game, Runnable completionCallback) {
        String gameName = game.getName();
        
        // Cancel any existing headstart task
        cancelHeadstartTask(gameName);
        
        // Show splash screen to all players for headstart start
        for (UUID playerId : game.getAllPlayers()) {
            Player player = Bukkit.getPlayer(playerId);
            if (player != null) {
                if (game.isRunner(player)) {
                    player.sendTitle(
                        ChatColor.GREEN + "Headstart Begins!",
                        ChatColor.GOLD + "Run and gather resources!",
                        10, 60, 20
                    );
                } else if (game.isHunter(player)) {
                    player.sendTitle(
                        ChatColor.RED + "Hunters Frozen",
                        ChatColor.GOLD + "You'll be released soon!",
                        10, 60, 20
                    );
                }
            }
        }
        
        // Schedule a task to run every second to count down
        int headstartDuration = game.getHeadstartDuration();
        int taskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, new Runnable() {
            private int secondsLeft = headstartDuration;
            
            @Override
            public void run() {
                if (secondsLeft <= 0) {
                    // Time's up - unfreeze hunters
                    unfreezeHunters(game);
                    
                    // Show splash screen to all players when hunters are released
                    for (UUID playerId : game.getAllPlayers()) {
                        Player player = Bukkit.getPlayer(playerId);
                        if (player != null) {
                            if (game.isRunner(player)) {
                                player.sendTitle(
                                    ChatColor.YELLOW + "Hunters Released!",
                                    ChatColor.RED + "They're coming for you now!",
                                    10, 60, 20
                                );
                            } else if (game.isHunter(player)) {
                                player.sendTitle(
                                    ChatColor.RED + "Hunt Begins!",
                                    ChatColor.GOLD + "Go catch those runners!",
                                    10, 60, 20
                                );
                            }
                        }
                    }
                    
                    // Call completion callback
                    completionCallback.run();
                    
                    // Cancel this task
                    cancelHeadstartTask(gameName);
                } else {
                    // Announce time remaining every 5 seconds or in the last 10 seconds
                    if (secondsLeft <= 10 || secondsLeft % 5 == 0) {
                        for (UUID playerId : game.getAllPlayers()) {
                            Player player = Bukkit.getPlayer(playerId);
                            if (player != null) {
                                player.sendMessage("§6Headstart time remaining: §e" + secondsLeft + " seconds");
                            }
                        }
                    }
                    
                    // Keep hunters frozen at their positions
                    for (UUID hunterId : game.getHunters()) {
                        Player hunter = Bukkit.getPlayer(hunterId);
                        if (hunter != null && isPlayerFrozen(hunter.getUniqueId())) {
                            Location frozenLoc = getFrozenLocation(hunter.getUniqueId());
                            if (frozenLoc != null) {
                                // Only teleport if they've moved
                                if (hunter.getLocation().distanceSquared(frozenLoc) > 0.01) {
                                    hunter.teleport(frozenLoc);
                                }
                            }
                        }
                    }
                    
                    secondsLeft--;
                }
            }
        }, 0L, 20L); // Run immediately, then every second (20 ticks)
        
        headstartTaskIds.put(gameName, taskId);
    }
    
    /**
     * Cancels a headstart task for a game.
     * 
     * @param gameName The name of the game
     */
    public void cancelHeadstartTask(String gameName) {
        Integer taskId = headstartTaskIds.remove(gameName);
        if (taskId != null) {
            Bukkit.getScheduler().cancelTask(taskId);
        }
    }
    
    /**
     * Gets the frozen location for a player.
     *
     * @param playerId The UUID of the player
     * @return The location where the player is frozen, or null if not frozen
     */
    public Location getFrozenLocation(UUID playerId) {
        return frozenHunterLocations.get(playerId);
    }

    /**
     * Freezes a hunter player during the headstart period by saving their spawn location.
     *
     * @param hunter The hunter to freeze
     */
    public void freezeHunter(Player hunter) {
        hunter.setGameMode(GameMode.SURVIVAL);
        hunter.setInvulnerable(true);
        
        // Save the hunter's current location as their spawn point
        frozenHunterLocations.put(hunter.getUniqueId(), hunter.getLocation().clone());
    }

    /**
     * Unfreezes hunters when the headstart period ends.
     *
     * @param game The game whose hunters should be unfrozen
     */
    public void unfreezeHunters(Game game) {
        for (UUID hunterId : game.getHunters()) {
            Player hunter = Bukkit.getPlayer(hunterId);
            if (hunter != null) {
                unfreezeHunter(hunter);
            }
        }
    }

    /**
     * Unfreezes a single player.
     *
     * @param player The player to unfreeze
     */
    public void unfreezeHunter(Player player) {
        if (isPlayerFrozen(player.getUniqueId())) {
            player.setInvulnerable(false);
            // Remove from frozen map
            frozenHunterLocations.remove(player.getUniqueId());
        }
    }

    /**
     * Checks if a player is currently frozen.
     *
     * @param playerId The UUID of the player to check
     * @return True if the player is frozen, false otherwise
     */
    public boolean isPlayerFrozen(UUID playerId) {
        return frozenHunterLocations.containsKey(playerId);
    }
} 