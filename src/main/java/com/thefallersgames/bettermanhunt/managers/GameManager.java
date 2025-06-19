package com.thefallersgames.bettermanhunt.managers;

import com.thefallersgames.bettermanhunt.Plugin;
import com.thefallersgames.bettermanhunt.models.Game;
import com.thefallersgames.bettermanhunt.models.GameState;
import com.thefallersgames.bettermanhunt.services.GameTaskService;
import com.thefallersgames.bettermanhunt.services.LobbyService;
import com.thefallersgames.bettermanhunt.tasks.CompassTask;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;
import java.util.concurrent.CompletableFuture;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import org.bukkit.Bukkit;

/**
 * Manages all Manhunt games and their lifecycles.
 * Delegates specific responsibilities to specialized managers.
 */
public class GameManager implements Listener {
    private final Plugin plugin;
    private final Logger logger;
    
    // Specialized managers
    private final GameRegistry gameRegistry;
    private final GameTaskService gameTaskService;
    private final PlayerStateManager playerStateManager;
    private final HeadstartManager headstartManager;
    private final GameLifecycleManager lifecycleManager;
    private final PlayerManager playerManager;
    private final GameSetupManager gameSetupManager;
    private final LobbyService lobbyService;
    private final StatsManager statsManager;

    /**
     * Constructs a new GameManager.
     *
     * @param plugin The plugin instance
     */
    public GameManager(Plugin plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        
        // Initialize registry and state managers
        this.gameRegistry = new GameRegistry();
        this.playerStateManager = new PlayerStateManager(plugin);
        
        // Get services
        this.headstartManager = plugin.getHeadstartManager();
        this.gameTaskService = plugin.getGameTaskService();
        this.gameSetupManager = this.gameTaskService.getGameSetupManager();
        this.lobbyService = plugin.getLobbyService();
        this.statsManager = plugin.getStatsManager();
        
        // Initialize managers that depend on other managers
        this.lifecycleManager = new GameLifecycleManager(
                plugin, gameRegistry, gameTaskService, playerStateManager, 
                headstartManager, gameSetupManager);
        this.playerManager = new PlayerManager(
                plugin, gameRegistry, gameTaskService, playerStateManager, headstartManager);
    }

    /**
     * Creates a new Manhunt game.
     *
     * @param name   The name of the game
     * @param owner  The player who owns the game
     * @param world  The world where the game will be played
     * @return True if the game was created, false if a game with that name already exists
     */
    public boolean createGame(String name, Player owner, World world) {
        return lifecycleManager.createGame(name, owner, world);
    }

    /**
     * Deletes a Manhunt game.
     *
     * @param gameName The name of the game to delete
     * @return True if the game was deleted, false if the game doesn't exist
     */
    public boolean deleteGame(String gameName) {
        return lifecycleManager.deleteGame(gameName);
    }

    /**
     * Adds a player to a game.
     *
     * @param player The player to add
     * @param game   The game to add the player to
     * @return True if the player was added, false if the player is already in a different game
     */
    public boolean addPlayerToGame(Player player, Game game) {
        // Don't allow joining games that are not in LOBBY state
        if (game.getState() != GameState.LOBBY) {
            player.sendMessage("§cCannot join - game is already in progress.");
            return false;
        }
        
        boolean added = playerManager.addPlayerToGame(player, game);
        
        if (added) {
            // Record that the player participated in a game
            statsManager.recordGamePlayed(player);
        }
        
        return added;
    }

    /**
     * Removes a player from their current game.
     *
     * @param player The player to remove
     * @return True if the player was removed, false if the player wasn't in a game
     */
    public boolean removePlayerFromGame(Player player) {
        Game game = playerManager.getPlayerGame(player);
        if (game == null) {
            return false;
        }
        
        // Track what team the player was on before removal
        boolean wasRunner = game.isRunner(player);
        boolean wasHunter = game.isHunter(player);
        
        // Remove the player
        boolean removed = playerManager.removePlayerFromGame(player);
        
        if (removed) {
            // Handle active games
            if (game.getState() == GameState.ACTIVE || 
                game.getState() == GameState.HEADSTART) {
                
                // Check game end conditions
                lifecycleManager.checkEndgameConditions(game);
            }
            // Handle lobby games
            else if (game.getState() == GameState.LOBBY) {
                // If the lobby is now empty, delete the game
                if (game.getAllPlayers().isEmpty()) {
                    logger.info("Lobby is empty. Deleting game: " + game.getName());
                    lifecycleManager.deleteGame(game.getName());
                }
                // If the owner left, assign a new owner or delete if empty
                else if (game.getOwner().equals(player.getUniqueId())) {
                    // Try to find a new owner
                    boolean ownerAssigned = false;
                    for (UUID playerId : game.getAllPlayers()) {
                        if (playerId != null) {
                            Player newOwner = org.bukkit.Bukkit.getPlayer(playerId);
                            if (newOwner != null) {
                                // Update the game with a new owner
                                gameRegistry.updateGameOwner(game.getName(), newOwner);
                                newOwner.sendMessage(org.bukkit.ChatColor.GREEN + "You are now the owner of game: " + game.getName());
                                ownerAssigned = true;
                                break;
                            }
                        }
                    }
                    
                    // If no owner could be assigned, delete the game
                    if (!ownerAssigned) {
                        logger.info("Owner left and no new owner could be assigned. Deleting game: " + game.getName());
                        lifecycleManager.deleteGame(game.getName());
                    }
                }
            }
            // For other game states, just check end conditions
            else if (game.getState() != GameState.RUNNERS_WON && 
                     game.getState() != GameState.HUNTERS_WON &&
                     game.getState() != GameState.ENDING &&
                     game.getState() != GameState.DELETING) {
                
                lifecycleManager.checkEndgameConditions(game);
            }
        }
        
        return removed;
    }

    /**
     * Updates the lobby boss bar for a game.
     *
     * @param game The game to update the boss bar for
     */
    public void updateLobbyBossBar(Game game) {
        if (game.getState() == GameState.LOBBY) {
            gameTaskService.updateLobbyBossBar(game);
        }
    }

    /**
     * Starts a Manhunt game.
     *
     * @param game The game to start
     * @return CompletableFuture<Boolean> that completes with true when the game is successfully started
     */
    public CompletableFuture<Boolean> startGame(Game game) {
        return lifecycleManager.startGame(game);
    }

    /**
     * Ends a Manhunt game.
     *
     * @param game The game to end
     * @param runnersWon Whether the runners won
     */
    public void endGame(Game game, boolean runnersWon) {
        // Record game result in stats before ending the game
        statsManager.recordGameResult(game, runnersWon);
        
        // End the game
        lifecycleManager.endGame(game, runnersWon);
    }

    /**
     * Handles a runner death.
     *
     * @param player The player who died
     */
    public void handleRunnerDeath(Player player) {
        Game game = playerManager.getPlayerGame(player);
        lifecycleManager.handleRunnerDeath(player, game);
    }
    
    /**
     * Check if game end conditions are met and end the game if necessary.
     * 
     * @param game The game to check
     */
    public void checkEndgameConditions(Game game) {
        lifecycleManager.checkEndgameConditions(game);
    }

    /**
     * Gets the game a player is in.
     *
     * @param player The player to check
     * @return The game the player is in, or null if not in any game
     */
    public Game getPlayerGame(Player player) {
        return playerManager.getPlayerGame(player);
    }

    /**
     * Gets a game by its name.
     *
     * @param gameName The name of the game
     * @return The game, or null if no game with that name exists
     */
    public Game getGame(String gameName) {
        return gameRegistry.getGame(gameName);
    }

    /**
     * Gets all games.
     *
     * @return A collection of all games
     */
    public Collection<Game> getAllGames() {
        return gameRegistry.getAllGames();
    }

    /**
     * Gets all games in lobby state.
     *
     * @return A list of all lobby games
     */
    public List<Game> getLobbyGames() {
        return gameRegistry.getGamesWithStates(GameState.LOBBY);
    }
    
    /**
     * Gets all active games (in HEADSTART or ACTIVE state).
     * 
     * @return A list of all active games
     */
    public List<Game> getActiveGames() {
        return gameRegistry.getGamesWithStates(GameState.HEADSTART, GameState.ACTIVE);
    }
    
    /**
     * Gets the compass task for a game.
     * 
     * @param gameName The name of the game
     * @return The compass task for the game, or null if none exists
     */
    public CompassTask getCompassTask(String gameName) {
        return gameTaskService.getCompassTask(gameName);
    }

    /**
     * Unfreezes hunters when the headstart period ends
     * 
     * @param game The game whose hunters should be unfrozen
     */
    public void unfreezeHunters(Game game) {
        headstartManager.unfreezeHunters(game);
    }

    /**
     * Updates a player's inventory with lobby items
     *
     * @param player The player to update
     */
    public void updatePlayerLobbyItems(Player player) {
        Game game = playerManager.getPlayerGame(player);
        if (game != null && game.getState() == GameState.LOBBY) {
            plugin.getGuiManager().giveLobbyItems(player, game);
        }
    }
    
    /**
     * Updates a player's inventory
     *
     * @param player The player to update
     */
    public void updatePlayerInventory(Player player) {
        playerManager.updatePlayerInventory(player);
    }
    
    /**
     * Cleans up all games and tasks.
     */
    public void cleanup() {
        logger.info("Starting cleanup of all games...");
        
        try {
            // Make a copy of all game names to avoid concurrent modification issues
            List<String> gameNames = new ArrayList<>(gameRegistry.getAllGameNames());
            
            // Delete all active games synchronously
            for (String gameName : gameNames) {
                try {
                    Game game = gameRegistry.getGame(gameName);
                    if (game != null) {
                        logger.info("Cleaning up game during shutdown: " + gameName);
                        
                        // Make sure all players are removed from the game properly
                        Set<UUID> allPlayers = new HashSet<>(game.getAllPlayers());
                        for (UUID playerId : allPlayers) {
                            Player player = Bukkit.getPlayer(playerId);
                            if (player != null) {
                                // Remove player from game tracking
                                game.removePlayer(player);
                                gameRegistry.removePlayerFromGame(playerId);
                                
                                // Make sure player is unfrozen if they were a hunter
                                if (headstartManager.isPlayerFrozen(playerId)) {
                                    headstartManager.unfreezeHunter(player);
                                }
                            }
                        }
                        
                        // Now delete the game
                        lifecycleManager.deleteGame(gameName);
                    }
                } catch (Exception e) {
                    logger.severe("Error cleaning up game " + gameName + ": " + e.getMessage());
                    // Continue with other games even if one fails
                }
            }
            
        } catch (Exception e) {
            logger.severe("Error during game cleanup: " + e.getMessage());
        }
        
        // Save stats before disabling
        statsManager.saveStats();
        
        logger.info("Game cleanup complete");
    }
    
    /**
     * Gets the headstart manager.
     * 
     * @return The headstart manager
     */
    public HeadstartManager getHeadstartManager() {
        return headstartManager;
    }
    
    /**
     * Gets the game setup manager.
     *
     * @return The game setup manager
     */
    public GameSetupManager getGameSetupManager() {
        return gameSetupManager;
    }
    
    /**
     * Gets the player state manager.
     *
     * @return The player state manager
     */
    public PlayerStateManager getPlayerStateManager() {
        return playerStateManager;
    }
    
    /**
     * Gets the stats manager.
     *
     * @return The stats manager
     */
    public StatsManager getStatsManager() {
        return statsManager;
    }

    /**
     * Updates the active game boss bar with current runner count.
     *
     * @param game The game to update the boss bar for
     */
    public void updateActiveGameBossBar(Game game) {
        if (game.getState() == GameState.ACTIVE) {
            gameTaskService.updateActiveGameBossBar(game);
        }
    }
    
    /**
     * Teleports a player to the lobby capsule for their game.
     * This is used when the game creator creates a new game.
     *
     * @param player The player to teleport
     * @param game The game
     * @return True if teleportation was successful, false otherwise
     */
    public boolean teleportToLobbyCapsule(Player player, Game game) {
        return lobbyService.teleportToLobbyCapsule(player, game);
    }
} 