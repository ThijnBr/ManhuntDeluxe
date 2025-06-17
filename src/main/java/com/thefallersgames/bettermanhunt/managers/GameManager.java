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
        return playerManager.addPlayerToGame(player, game);
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
            if (game.getState() == GameState.ACTIVE) {
                // Check if this was a runner leaving
                if (wasRunner) {
                    // If no runners left, hunters win
                    if (game.getRunners().isEmpty()) {
                        logger.info("Last runner left the game. Hunters win!");
                        lifecycleManager.endGame(game, false); // Hunters win
                    }
                }
                // Check if this was a hunter leaving
                else if (wasHunter) {
                    // If no hunters left, runners win
                    if (game.getHunters().isEmpty()) {
                        logger.info("Last hunter left the game. Runners win!");
                        lifecycleManager.endGame(game, true); // Runners win
                    }
                }
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
     * @return True if the game was started, false if there's an issue starting the game
     */
    public boolean startGame(Game game) {
        return lifecycleManager.startGame(game);
    }

    /**
     * Ends a Manhunt game.
     *
     * @param game The game to end
     * @param runnersWon Whether the runners won
     */
    public void endGame(Game game, boolean runnersWon) {
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
     * Gets all games in lobby or active state.
     *
     * @return A list of all lobby or active games
     */
    public List<Game> getActiveOrLobbyGames() {
        return gameRegistry.getGamesWithStates(GameState.LOBBY, GameState.ACTIVE);
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
        for (String gameName : gameRegistry.getAllGameNames()) {
            deleteGame(gameName);
        }
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