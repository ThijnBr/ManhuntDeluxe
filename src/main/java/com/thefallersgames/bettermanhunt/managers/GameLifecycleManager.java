package com.thefallersgames.bettermanhunt.managers;

import com.thefallersgames.bettermanhunt.Plugin;
import com.thefallersgames.bettermanhunt.services.WorldManagementService;
import com.thefallersgames.bettermanhunt.services.GameTaskService;
import com.thefallersgames.bettermanhunt.models.Game;
import com.thefallersgames.bettermanhunt.models.GameState;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.World;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.ChatColor;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Manages the lifecycle of Manhunt games (creation, start, end, deletion).
 */
public class GameLifecycleManager {
    private final Plugin plugin;
    private final Logger logger;
    private final GameRegistry gameRegistry;
    private final GameTaskService gameTaskService;
    private final PlayerStateManager playerStateManager;
    private final HeadstartManager headstartManager;
    private final GameSetupManager gameSetupManager;
    private final WorldManagementService worldManagementService;
    private final String worldsFolder;

    /**
     * Constructs a new GameLifecycleManager.
     *
     * @param plugin The plugin instance
     * @param gameRegistry The game registry to use
     * @param gameTaskService The game task service to use
     * @param playerStateManager The player state manager to use
     * @param headstartManager The headstart manager to use
     * @param gameSetupManager The game setup manager to use
     */
    public GameLifecycleManager(
            Plugin plugin,
            GameRegistry gameRegistry,
            GameTaskService gameTaskService,
            PlayerStateManager playerStateManager,
            HeadstartManager headstartManager,
            GameSetupManager gameSetupManager) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        this.gameRegistry = gameRegistry;
        this.gameTaskService = gameTaskService;
        this.playerStateManager = playerStateManager;
        this.headstartManager = headstartManager;
        this.gameSetupManager = gameSetupManager;
        this.worldManagementService = plugin.getWorldManagementService();
        this.worldsFolder = plugin.getConfig().getString("custom-worlds-folder", "ManhuntWorlds");
    }

    /**
     * Creates a new Manhunt game.
     *
     * @param name The name of the game
     * @param owner The player who owns the game
     * @param world The world where the game will be played
     * @return True if the game was created, false if a game with that name already exists or the world is already in use
     */
    public boolean createGame(String name, Player owner, World world) {
        if (gameRegistry.gameExists(name)) {
            return false;
        }

        Game game = new Game(name, owner, world);
        gameRegistry.registerGame(game, owner);
        game.addRunner(owner); // Default to runner team
        
        // Create lobby boss bar
        gameTaskService.createLobbyBossBar(game);
        
        logger.info("Created new game: " + name + " by " + owner.getName());
        return true;
    }

    /**
     * Deletes a Manhunt game.
     *
     * @param gameName The name of the game to delete
     * @return True if the game was deleted, false if the game doesn't exist
     */
    public boolean deleteGame(String gameName) {
        Game game = gameRegistry.getGame(gameName);
        if (game == null) {
            return false;
        }

        // Get the world name before cleaning up players
        String worldName = game.getWorld().getName();
        boolean isDynamicallyGenerated = worldName.contains(worldsFolder + "/");

        // Set game state to DELETING if not already in ENDING or DELETING state
        if (game.getState() != GameState.ENDING && game.getState() != GameState.DELETING) {
            game.setState(GameState.DELETING);
        }

        // Create a copy of player UUIDs to avoid concurrent modification
        Set<UUID> allPlayers = new HashSet<>(game.getAllPlayers());
        
        // Clean up players in the game
        for (UUID playerId : allPlayers) {
            Player player = Bukkit.getPlayer(playerId);
            if (player != null) {
                // Make sure player is unfrozen if they were a hunter
                if (headstartManager.isPlayerFrozen(playerId)) {
                    headstartManager.unfreezeHunter(player);
                }
                
                // First remove player from the game to ensure no more game events affect them
                game.removePlayer(player);
                gameRegistry.removePlayerFromGame(playerId);
                
                // Then restore their state
                playerStateManager.restorePlayerState(player);
                
                // Remove from boss bar
                gameTaskService.removePlayerFromBossBar(gameName, player);
                
                player.sendMessage("§cThe game has been deleted.");
            }
        }
        
        // Clean up game resources
        gameTaskService.cleanupGameTasks(gameName);
        gameRegistry.unregisterGame(gameName);
        
        // If this was a dynamically generated world, delete it
        if (isDynamicallyGenerated) {
            // Check if the plugin is being disabled
            if (!plugin.isEnabled()) {
                // Plugin is being disabled, delete the world synchronously
                try {
                    // Still async but we'll wait for the result
                    boolean deleted = worldManagementService.deleteWorld(worldName).get();
                    if (deleted) {
                        logger.info("Deleted dynamically generated world during shutdown: " + worldName);
                    } else {
                        logger.warning("Failed to delete dynamically generated world during shutdown: " + worldName + 
                                    ". The server might need to clean it up on next restart.");
                    }
                } catch (Exception e) {
                    logger.severe("Error deleting world during shutdown: " + worldName + " - " + e.getMessage());
                }
            } else {
                // Plugin is still enabled, schedule world deletion after a short delay
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    worldManagementService.deleteWorld(worldName)
                        .thenAccept(deleted -> {
                            if (deleted) {
                                logger.info("Deleted dynamically generated world: " + worldName);
                            } else {
                                logger.warning("Failed to delete dynamically generated world: " + worldName);
                                
                                // Only attempt a second time if the plugin is still enabled
                                if (plugin.isEnabled()) {
                                    // Attempt to force delete after an additional delay if first attempt failed
                                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                                        if (plugin.isEnabled()) {
                                            worldManagementService.deleteWorld(worldName)
                                                .thenAccept(forcedDelete -> {
                                                    if (forcedDelete) {
                                                        logger.info("Successfully force-deleted world: " + worldName);
                                                    } else {
                                                        logger.severe("Failed to force-delete world: " + worldName + ". Manual cleanup may be required.");
                                                    }
                                                });
                                        }
                                    }, 100L); // 5 second additional delay
                                }
                            }
                        });
                }, 40L); // 2 second delay
            }
        }
        
        logger.info("Deleted game: " + gameName);
        return true;
    }

    /**
     * Starts a Manhunt game.
     *
     * @param game The game to start
     * @return CompletableFuture<Boolean> that completes with true if started successfully, false otherwise
     */
    public CompletableFuture<Boolean> startGame(Game game) {
        CompletableFuture<Boolean> result = new CompletableFuture<>();
        
        if (game.getState() != GameState.LOBBY) {
            result.complete(false);
            return result;
        }
        
        if (game.getRunners().isEmpty() || game.getHunters().isEmpty()) {
            result.complete(false); // Need at least one runner and one hunter
            return result;
        }

        // Set to STARTING state
        game.setState(GameState.STARTING);
        
        // Get all players before teleportation starts
        Set<UUID> allPlayers = new HashSet<>(game.getAllPlayers());
        AtomicInteger pendingTeleports = new AtomicInteger(allPlayers.size());
        
        // Set up boss bar for the starting state
        gameTaskService.setupStartingBossBar(game);
        
        // Now teleport all players from the lobby capsule to the spawn position
        game.setState(GameState.TELEPORTING);
        
        // Get spawn location - this is where players will be teleported
        Location spawnLocation = game.getSpawnLocation();
        
        for (UUID playerId : allPlayers) {
            Player player = Bukkit.getPlayer(playerId);
            if (player == null) {
                // Decrement counter for invalid players
                if (pendingTeleports.decrementAndGet() == 0) {
                    finalizeGameStart(game, result);
                }
                continue;
            }
            
            // Schedule teleport on the main thread - from lobby capsule to spawn position
            Bukkit.getScheduler().runTask(plugin, () -> {
                boolean teleportSuccess = player.teleport(spawnLocation);
                
                if (!teleportSuccess) {
                    player.sendMessage("§cFailed to teleport to the game spawn position!");
                    // Handle teleport failure - don't remove from game, but log the issue
                    logger.warning("Failed to teleport player " + player.getName() + " to spawn position");
                }
                
                // Continue with remaining teleports regardless
                if (pendingTeleports.decrementAndGet() == 0) {
                    // All teleports attempted, check if we still have enough players
                    if (game.getRunners().isEmpty() || game.getHunters().isEmpty()) {
                        // Not enough players after teleportation, cancel game
                        Bukkit.getScheduler().runTaskLater(plugin, () -> {
                            game.setState(GameState.ENDING);
                            deleteGame(game.getName());
                            result.complete(false);
                        }, 1L);
                    } else {
                        // Proceed with game start
                        finalizeGameStart(game, result);
                    }
                }
            });
        }
        
        return result;
    }
    
    /**
     * Finalizes the game start after all teleportations are complete
     */
    private void finalizeGameStart(Game game, CompletableFuture<Boolean> result) {
        // Switch to headstart mode if headstart is enabled
        if (game.getHeadstartDuration() > 0) {
            game.setState(GameState.HEADSTART);
            
            // Set up the headstart mechanics
            gameTaskService.setupHeadstart(game, (g, player, isHunter) -> gameSetupManager.setupPlayer(g, player, isHunter));
            
            // Start the headstart timer which will transition to ACTIVE when done
            headstartManager.startHeadstart(game, () -> {
                // This will be called when headstart completes
                game.setState(GameState.ACTIVE);
                gameTaskService.transitionToActiveState(game);
            });
        } else {
            // No headstart - go directly to active state
            game.setState(GameState.ACTIVE);
            gameTaskService.setupGameStart(game, (g, player, isHunter) -> gameSetupManager.setupPlayer(g, player, isHunter));
        }
        
        // Mark the start as successful
        result.complete(true);
        logger.info("Started game: " + game.getName());
    }

    /**
     * Ends a Manhunt game.
     *
     * @param game The game to end
     * @param runnersWon Whether the runners won
     */
    public void endGame(Game game, boolean runnersWon) {
        // Don't do anything if the game is already in an ending state
        if (game.getState() == GameState.RUNNERS_WON || 
            game.getState() == GameState.HUNTERS_WON || 
            game.getState() == GameState.ENDING || 
            game.getState() == GameState.DELETING) {
            return;
        }
        
        // Set appropriate game state
        game.setState(runnersWon ? GameState.RUNNERS_WON : GameState.HUNTERS_WON);
        
        // Unfreeze any hunters that might still be frozen
        headstartManager.unfreezeHunters(game);
        
        // Update boss bar and display winner
        gameTaskService.handleGameEnd(game, runnersWon);
        
        // Show splash screen to all players for game end
        for (UUID playerId : game.getAllPlayers()) {
            Player player = Bukkit.getPlayer(playerId);
            if (player != null) {
                if (runnersWon) {
                    // Runners won
                    player.sendTitle(
                        ChatColor.GREEN + "GAME OVER",
                        ChatColor.GOLD + "The Runners have won!",
                        20, 100, 20
                    );
                } else {
                    // Hunters won
                    player.sendTitle(
                        ChatColor.RED + "GAME OVER",
                        ChatColor.GOLD + "The Hunters have won!",
                        20, 100, 20
                    );
                }
            }
        }
        
        // Schedule task to clean up the game after showing results
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            // Transition to ENDING state
            game.setState(GameState.ENDING);
            
            // Start the cleanup process
            cleanup(game);
        }, 200L); // 10 seconds
        
        logger.info("Ended game: " + game.getName() + " - Runners won: " + runnersWon);
    }
    
    /**
     * Cleans up a game and removes all players
     */
    private void cleanup(Game game) {
        // Make a copy of all players to avoid concurrent modification issues
        Set<UUID> allPlayers = new HashSet<>(game.getAllPlayers());
        
        // Track how many players we're attempting to teleport out
        AtomicInteger pendingTeleports = new AtomicInteger(allPlayers.size());
        
        if (pendingTeleports.get() == 0) {
            // No players to teleport, proceed to delete game
            deleteGame(game.getName());
            return;
        }
        
        // Process all players
        for (UUID playerId : allPlayers) {
            Player player = Bukkit.getPlayer(playerId);
            if (player != null) {
                // Only schedule if plugin is enabled
                if (plugin.isEnabled()) {
                    // Schedule restoration on main thread
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        // First remove from the game
                        game.removePlayer(player);
                        gameRegistry.removePlayerFromGame(player.getUniqueId());
                        
                        // Then restore player state (which will teleport them to lobby/restore location)
                        playerStateManager.restorePlayerState(player);
                        
                        // Remove from boss bar
                        gameTaskService.removePlayerFromBossBar(game.getName(), player);
                        
                        // Check if all teleports are complete
                        if (pendingTeleports.decrementAndGet() == 0) {
                            // Only schedule deletion if plugin is still enabled
                            if (plugin.isEnabled()) {
                                // Schedule game deletion 
                                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                                    if (plugin.isEnabled()) {
                                        deleteGame(game.getName());
                                    } else {
                                        // Direct deletion without scheduling
                                        deleteGame(game.getName());
                                    }
                                }, 20L); // 1 second delay after teleports
                            } else {
                                // Direct deletion without scheduling
                                deleteGame(game.getName());
                            }
                        }
                    });
                } else {
                    // Plugin is being disabled, do direct cleanup
                    game.removePlayer(player);
                    gameRegistry.removePlayerFromGame(player.getUniqueId());
                    playerStateManager.restorePlayerState(player);
                    gameTaskService.removePlayerFromBossBar(game.getName(), player);
                    
                    // Decrement counter
                    if (pendingTeleports.decrementAndGet() == 0) {
                        // Direct deletion without scheduling
                        deleteGame(game.getName());
                    }
                }
            } else {
                // Player is null, just decrement counter
                if (pendingTeleports.decrementAndGet() == 0) {
                    // All players processed
                    if (plugin.isEnabled()) {
                        // Schedule deletion if plugin is still enabled
                        Bukkit.getScheduler().runTaskLater(plugin, () -> {
                            if (plugin.isEnabled()) {
                                deleteGame(game.getName());
                            } else {
                                // Direct deletion without scheduling
                                deleteGame(game.getName());
                            }
                        }, 20L);
                    } else {
                        // Direct deletion without scheduling
                        deleteGame(game.getName());
                    }
                }
            }
        }
    }

    /**
     * Handles a runner death.
     *
     * @param player The player who died
     * @param game The game the player is in
     */
    public void handleRunnerDeath(Player player, Game game) {
        if (game == null || 
            (game.getState() != GameState.ACTIVE && 
             game.getState() != GameState.HEADSTART) || 
            !game.isRunner(player)) {
            return;
        }
        
        // Make player a spectator
        game.addSpectator(player);
        player.setGameMode(GameMode.SPECTATOR);
        
        // Check if this was the last runner
        if (game.getRunners().isEmpty()) {
            // Hunters win
            endGame(game, false);
        }
    }
    
    /**
     * Checks for endgame conditions and triggers game end if met
     * 
     * @param game The game to check
     */
    public void checkEndgameConditions(Game game) {
        if (game == null || 
            (game.getState() != GameState.ACTIVE && 
             game.getState() != GameState.HEADSTART)) {
            return;
        }
        
        // Check game ending conditions
        if (game.getRunners().isEmpty()) {
            logger.info("No runners left in the game. Hunters win!");
            endGame(game, false); // Hunters win
            return;
        }
        
        if (game.getHunters().isEmpty()) {
            logger.info("No hunters left in the game. Runners win!");
            endGame(game, true); // Runners win
            return;
        }
    }
} 