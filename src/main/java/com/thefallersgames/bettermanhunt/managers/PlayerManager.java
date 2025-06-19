package com.thefallersgames.bettermanhunt.managers;

import com.thefallersgames.bettermanhunt.Plugin;
import com.thefallersgames.bettermanhunt.models.Game;
import com.thefallersgames.bettermanhunt.models.GameState;
import com.thefallersgames.bettermanhunt.services.GameTaskService;
import com.thefallersgames.bettermanhunt.services.LobbyService;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.ChatColor;
import org.bukkit.scheduler.BukkitTask;

import java.util.UUID;
import java.util.logging.Logger;
import java.util.concurrent.CompletableFuture;
import java.util.HashMap;
import java.util.Map;

/**
 * Manages players in Manhunt games (adding, removing, etc.).
 */
public class PlayerManager {
    private final Plugin plugin;
    private final Logger logger;
    private final GameRegistry gameRegistry;
    private final GameTaskService gameTaskService;
    private final PlayerStateManager playerStateManager;
    private final HeadstartManager headstartManager;
    private final LobbyService lobbyService;
    
    // Map to track teleportation tasks
    private final Map<UUID, BukkitTask> pendingTeleports = new HashMap<>();

    /**
     * Constructs a new PlayerManager.
     *
     * @param plugin The plugin instance
     * @param gameRegistry The game registry to use
     * @param gameTaskService The game task service to use
     * @param playerStateManager The player state manager to use
     * @param headstartManager The headstart manager to use
     */
    public PlayerManager(
            Plugin plugin,
            GameRegistry gameRegistry,
            GameTaskService gameTaskService,
            PlayerStateManager playerStateManager,
            HeadstartManager headstartManager) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        this.gameRegistry = gameRegistry;
        this.gameTaskService = gameTaskService;
        this.playerStateManager = playerStateManager;
        this.headstartManager = headstartManager;
        this.lobbyService = plugin.getLobbyService();
    }

    /**
     * Adds a player to a game.
     *
     * @param player The player to add
     * @param game The game to add the player to
     * @return True if the player was added, false if the player is already in a different game
     */
    public boolean addPlayerToGame(Player player, Game game) {
        UUID playerId = player.getUniqueId();
        String currentGame = gameRegistry.getPlayerGame(playerId);
        
        // If player is already in a game, remove them first
        if (currentGame != null) {
            if (currentGame.equals(game.getName())) {
                return true; // Already in this game
            }
            removePlayerFromGame(player);
        }
        
        // Only allow joining lobby games
        if (game.getState() != GameState.LOBBY) {
            player.sendMessage(ChatColor.RED + "Cannot join - game is already in progress.");
            return false;
        }
        
        // Save player's current inventory and state
        playerStateManager.savePlayerState(player);
        
        // Cancel any pending teleport tasks
        cancelPendingTeleport(playerId);
        
        // Auto-assign player to a team if they're not already on one
        if (!game.isHunter(player) && !game.isRunner(player) && !game.isSpectator(player)) {
            // If there are more hunters than runners, make player a runner
            if (game.getHunters().size() > game.getRunners().size()) {
                game.addRunner(player);
                player.sendMessage("§aYou have been assigned to the §bRunner §ateam! Use the team selector item to change teams.");
            } else {
                // Otherwise, make player a hunter
                game.addHunter(player);
                player.sendMessage("§aYou have been assigned to the §cHunter §ateam! Use the team selector item to change teams.");
            }
        }
        
        // Set lobby-specific player states
        lobbyService.setupLobbyPlayerState(player);
        
        // First attempt teleportation to the lobby capsule in the game world
        CompletableFuture<Boolean> teleportFuture = new CompletableFuture<>();
        
        // Use task to attempt teleportation
        BukkitTask task = Bukkit.getScheduler().runTask(plugin, () -> {
            // Try to teleport to the lobby capsule in the game world
            boolean teleportSuccess = lobbyService.teleportToLobbyCapsule(player, game);
            
            if (teleportSuccess) {
                // Add player to game registry
                gameRegistry.addPlayerToGame(player, game);
                
                // Give lobby items to the player
                plugin.getGuiManager().giveLobbyItems(player, game);
                
                // Add to lobby boss bar and update it
                gameTaskService.addPlayerToBossBar(game.getName(), player);
                gameTaskService.updateLobbyBossBar(game);
                
                logger.info("Added player " + player.getName() + " to game " + game.getName());
                teleportFuture.complete(true);
            } else {
                // Teleport failed, restore player state and abort
                playerStateManager.restorePlayerState(player);
                player.sendMessage(ChatColor.RED + "Failed to teleport to the game lobby capsule. Game join aborted.");
                teleportFuture.complete(false);
            }
        });
        
        pendingTeleports.put(playerId, task);
        
        return true;
    }

    /**
     * Removes a player from their current game.
     *
     * @param player The player to remove
     * @return True if the player was removed, false if the player wasn't in a game
     */
    public boolean removePlayerFromGame(Player player) {
        UUID playerId = player.getUniqueId();
        String gameName = gameRegistry.getPlayerGame(playerId);
        
        if (gameName == null) {
            return false;
        }
        
        // Cancel any pending teleports for this player
        cancelPendingTeleport(playerId);
        
        Game game = gameRegistry.getGame(gameName);
        if (game != null) {
            // Make sure player is unfrozen if they were a hunter
            if (headstartManager.isPlayerFrozen(playerId)) {
                headstartManager.unfreezeHunter(player);
            }
            
            // Remove from the game object
            game.removePlayer(player);
            
            // Remove from game registry
            gameRegistry.removePlayerFromGame(playerId);
            
            // Remove player from boss bar
            gameTaskService.removePlayerFromBossBar(gameName, player);
            
            // Update the lobby boss bar if in lobby state
            if (game.getState() == GameState.LOBBY) {
                gameTaskService.updateLobbyBossBar(game);
            }
            
            // Schedule task to teleport the player out and restore state
            Bukkit.getScheduler().runTask(plugin, () -> {
                // Restore player's state which includes teleportation
                playerStateManager.restorePlayerState(player);
            });
        }
        
        logger.info("Removed player " + player.getName() + " from game " + gameName);
        return true;
    }
    
    /**
     * Cancels any pending teleport tasks for a player.
     * 
     * @param playerId The UUID of the player
     */
    private void cancelPendingTeleport(UUID playerId) {
        BukkitTask task = pendingTeleports.remove(playerId);
        if (task != null && !task.isCancelled()) {
            task.cancel();
        }
    }

    /**
     * Gets the game a player is in.
     *
     * @param player The player to check
     * @return The game the player is in, or null if not in any game
     */
    public Game getPlayerGame(Player player) {
        String gameName = gameRegistry.getPlayerGame(player.getUniqueId());
        if (gameName == null) {
            return null;
        }
        return gameRegistry.getGame(gameName);
    }

    /**
     * Updates a player's inventory with lobby items
     *
     * @param player The player to update
     */
    public void updatePlayerInventory(Player player) {
        Game game = getPlayerGame(player);
        if (game != null && game.getState() == GameState.LOBBY) {
            // Use the GuiManager to give lobby items to the player
            plugin.getGuiManager().giveLobbyItems(player, game);
            logger.info("Updated inventory for " + player.getName() + " in game " + game.getName());
        }
    }
} 