package com.thefallersgames.bettermanhunt.managers;

import com.thefallersgames.bettermanhunt.Plugin;
import com.thefallersgames.bettermanhunt.services.WorldManagementService;
import com.thefallersgames.bettermanhunt.models.Game;
import com.thefallersgames.bettermanhunt.models.GameState;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.UUID;
import java.util.logging.Logger;

/**
 * Manages the lifecycle of Manhunt games (creation, start, end, deletion).
 */
public class GameLifecycleManager {
    private final Plugin plugin;
    private final Logger logger;
    private final GameRegistry gameRegistry;
    private final TaskManager taskManager;
    private final PlayerStateManager playerStateManager;
    private final HeadstartManager headstartManager;
    private final GameSetupManager gameSetupManager;
    private final WorldManagementService worldManagementService;

    /**
     * Constructs a new GameLifecycleManager.
     *
     * @param plugin The plugin instance
     * @param gameRegistry The game registry to use
     * @param taskManager The task manager to use
     * @param playerStateManager The player state manager to use
     * @param headstartManager The headstart manager to use
     * @param gameSetupManager The game setup manager to use
     */
    public GameLifecycleManager(
            Plugin plugin,
            GameRegistry gameRegistry,
            TaskManager taskManager,
            PlayerStateManager playerStateManager,
            HeadstartManager headstartManager,
            GameSetupManager gameSetupManager) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        this.gameRegistry = gameRegistry;
        this.taskManager = taskManager;
        this.playerStateManager = playerStateManager;
        this.headstartManager = headstartManager;
        this.gameSetupManager = gameSetupManager;
        this.worldManagementService = plugin.getWorldManagementService();
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
        taskManager.createLobbyBossBar(game);
        
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
        boolean isDynamicallyGenerated = worldName.startsWith("manhunt_");

        // Clean up players in the game
        for (UUID playerId : game.getAllPlayers()) {
            Player player = Bukkit.getPlayer(playerId);
            if (player != null) {
                playerStateManager.restorePlayerState(player);
            }
        }
        
        // Clean up game resources
        taskManager.cleanupGameTasks(gameName);
        gameRegistry.unregisterGame(gameName);
        
        // If this was a dynamically generated world, delete it
        if (isDynamicallyGenerated) {
            // Schedule world deletion after a short delay to ensure all players are out
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                boolean deleted = worldManagementService.deleteWorld(worldName);
                if (deleted) {
                    logger.info("Deleted dynamically generated world: " + worldName);
                } else {
                    logger.warning("Failed to delete dynamically generated world: " + worldName);
                }
            }, 20L); // 1 second delay
        }
        
        logger.info("Deleted game: " + gameName);
        return true;
    }

    /**
     * Starts a Manhunt game.
     *
     * @param game The game to start
     * @return True if the game was started, false if there's an issue starting the game
     */
    public boolean startGame(Game game) {
        if (game.getState() != GameState.LOBBY) {
            return false;
        }
        
        if (game.getRunners().isEmpty() || game.getHunters().isEmpty()) {
            return false; // Need at least one runner and one hunter
        }
        
        // Make sure all players can be teleported to the game world
        boolean allPlayersCanTeleport = true;
        String failedPlayerName = null;
        
        // Check hunters
        for (UUID playerId : game.getHunters()) {
            Player player = Bukkit.getPlayer(playerId);
            if (player != null) {
                if (!worldManagementService.isWorldAccessibleForTeleport(game.getWorld(), player)) {
                    allPlayersCanTeleport = false;
                    failedPlayerName = player.getName();
                    break;
                }
            }
        }

        // Set up the game
        game.setState(GameState.ACTIVE);
        
        // Set up boss bar and prepare players
        taskManager.setupGameStart(game, (g, player, isHunter) -> gameSetupManager.setupPlayer(g, player, isHunter));
        
        logger.info("Started game: " + game.getName());
        return true;
    }

    /**
     * Ends a Manhunt game.
     *
     * @param game The game to end
     * @param runnersWon Whether the runners won
     */
    public void endGame(Game game, boolean runnersWon) {
        if (game.getState() == GameState.GAME_ENDED) {
            return;
        }
        
        game.setState(GameState.GAME_ENDED);
        
        // Unfreeze any hunters that might still be frozen
        headstartManager.unfreezeHunters(game);
        
        // Update boss bar and display winner
        taskManager.handleGameEnd(game, runnersWon);
        
        // Schedule task to reset game after some time
        Bukkit.getScheduler().runTaskLater(plugin, () -> resetGame(game), 200L); // 10 seconds
        
        logger.info("Ended game: " + game.getName() + " - Runners won: " + runnersWon);
    }
    
    /**
     * Resets a game to lobby state
     */
    private void resetGame(Game game) {
        // Reset game state to lobby if it still exists
        if (gameRegistry.gameExists(game.getName())) {
            game.setState(GameState.LOBBY);
            
            // Remove boss bar
            taskManager.removeBossBar(game.getName());
            
            // Reset players and remove from game
            for (UUID playerId : game.getAllPlayers()) {
                Player player = Bukkit.getPlayer(playerId);
                if (player != null) {
                    playerStateManager.restorePlayerState(player);
                    // Remove player from the game
                    game.removePlayer(player);
                }
            }
            
            // Delete the game
            deleteGame(game.getName());
            
            logger.info("Reset and removed game: " + game.getName());
        }
    }

    /**
     * Handles a runner death.
     *
     * @param player The player who died
     * @param game The game the player is in
     */
    public void handleRunnerDeath(Player player, Game game) {
        if (game == null || game.getState() != GameState.ACTIVE || !game.isRunner(player)) {
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
} 