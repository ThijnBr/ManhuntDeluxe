package com.thefallersgames.bettermanhunt.managers;

import com.thefallersgames.bettermanhunt.Plugin;
import com.thefallersgames.bettermanhunt.models.Game;
import com.thefallersgames.bettermanhunt.models.GameState;
import org.bukkit.entity.Player;
import org.bukkit.GameMode;
import org.bukkit.ChatColor;
import org.bukkit.Location;

import java.util.UUID;
import java.util.logging.Logger;

/**
 * Manages players in Manhunt games (adding, removing, etc.).
 */
public class PlayerManager {
    private final Plugin plugin;
    private final Logger logger;
    private final GameRegistry gameRegistry;
    private final TaskManager taskManager;
    private final PlayerStateManager playerStateManager;
    private final HeadstartManager headstartManager;
    private final com.thefallersgames.bettermanhunt.services.WorldManagementService worldManagementService;

    /**
     * Constructs a new PlayerManager.
     *
     * @param plugin The plugin instance
     * @param gameRegistry The game registry to use
     * @param taskManager The task manager to use
     * @param playerStateManager The player state manager to use
     * @param headstartManager The headstart manager to use
     */
    public PlayerManager(
            Plugin plugin,
            GameRegistry gameRegistry,
            TaskManager taskManager,
            PlayerStateManager playerStateManager,
            HeadstartManager headstartManager) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        this.gameRegistry = gameRegistry;
        this.taskManager = taskManager;
        this.playerStateManager = playerStateManager;
        this.headstartManager = headstartManager;
        this.worldManagementService = plugin.getWorldManagementService();
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

        // Check if the player can teleport to the world
        if (!worldManagementService.isWorldAccessibleForTeleport(game.getWorld(), player)) {
            player.sendMessage(ChatColor.RED + "You cannot be teleported to the game world. Game join aborted.");
            return false;
        }
        
        // Store the player's original location before saving state
        Location originalLocation = player.getLocation().clone();
        
        // Save player's current inventory and state
        playerStateManager.savePlayerState(player);
        
        if (game.getState() == GameState.LOBBY) {
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
            setupLobbyPlayerState(player);
            
            // First teleport player to the glass capsule above the world spawn
            // Only continue if teleportation succeeds
            boolean teleportSuccess = teleportToLobbyCapsule(player, game);
            if (!teleportSuccess) {
                // Revert team assignment and return false
                game.removePlayer(player);
                playerStateManager.restorePlayerState(player);
                player.sendMessage(ChatColor.RED + "Failed to teleport to the game world. Game join aborted.");
                return false;
            }
            
            // Then give lobby items to the player
            plugin.getGuiManager().giveLobbyItems(player, game);
        } else {
            // We already checked if teleportation is possible, so just teleport them
            try {
                boolean teleportSuccess = player.teleport(game.getWorld().getSpawnLocation());
                if (!teleportSuccess) {
                    // Teleport failed, restore player state and abort
                    playerStateManager.restorePlayerState(player);
                    player.sendMessage(ChatColor.RED + "Failed to teleport to the game world. Game join aborted.");
                    return false;
                }
            } catch (Exception e) {
                // This shouldn't happen as we already checked, but just in case
                logger.warning("Failed to teleport player " + player.getName() + " to game world: " + e.getMessage());
                playerStateManager.restorePlayerState(player);
                player.sendMessage(ChatColor.RED + "Failed to teleport to the game world. Game join aborted.");
                return false;
            }
        }
        
        // Add player to game registry ONLY if all previous steps succeeded
        gameRegistry.addPlayerToGame(player, game);
        
        // Add boss bar if game is active or create one if in lobby
        if (game.getState() != GameState.LOBBY) {
            taskManager.addPlayerToBossBar(game.getName(), player);
        } else {
            // Add to lobby boss bar and update it
            taskManager.addPlayerToBossBar(game.getName(), player);
            taskManager.updateLobbyBossBar(game);
        }
        
        logger.info("Added player " + player.getName() + " to game " + game.getName());
        return true;
    }

    /**
     * Teleports a player to the glass capsule lobby 100 blocks above the world spawn.
     * Creates the capsule if it doesn't exist.
     *
     * @param player The player to teleport
     * @param game The game the player is in
     * @return True if teleportation was successful, false otherwise
     */
    public boolean teleportToLobbyCapsule(Player player, Game game) {
        try {
            // Get the world and spawn location
            org.bukkit.World world = game.getWorld();
            org.bukkit.Location worldSpawn = world.getSpawnLocation().clone();
            
            // Create capsule 100 blocks above world spawn
            org.bukkit.Location capsuleCenter = worldSpawn.clone().add(0, 100, 0);
            
            // Check if capsule already exists, if not, create it
            if (!isCapsulePresent(capsuleCenter)) {
                createGlassCapsule(capsuleCenter);
            }
            
            // Teleport player inside the capsule
            org.bukkit.Location teleportLocation = capsuleCenter.clone().add(0, 1, 0);
            return player.teleport(teleportLocation);
        } catch (Exception e) {
            logger.warning("Error teleporting player to lobby capsule: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Checks if a glass capsule is already present at the given location.
     *
     * @param center The center location of the capsule
     * @return True if the capsule exists
     */
    private boolean isCapsulePresent(org.bukkit.Location center) {
        // Simple check: just check the floor block
        return center.clone().subtract(0, 1, 0).getBlock().getType() == org.bukkit.Material.GLASS;
    }
    
    /**
     * Creates a glass capsule around the given center location.
     *
     * @param center The center location for the capsule
     */
    private void createGlassCapsule(org.bukkit.Location center) {
        // Create a 5x5x3 glass capsule (5x5 base, 3 blocks high)
        org.bukkit.World world = center.getWorld();
        
        // Create the capsule
        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                for (int y = -1; y <= 2; y++) {
                    // Skip the center blocks to make an open space
                    if (y > -1 && y < 2 && Math.abs(x) < 2 && Math.abs(z) < 2) {
                        continue;
                    }
                    
                    org.bukkit.Location blockLoc = center.clone().add(x, y, z);
                    blockLoc.getBlock().setType(org.bukkit.Material.GLASS);
                }
            }
        }
        
        // Add some light
        center.clone().add(0, 2, 0).getBlock().setType(org.bukkit.Material.GLOWSTONE);
    }

    /**
     * Sets up a player's state for the lobby.
     *
     * @param player The player to set up
     */
    private void setupLobbyPlayerState(Player player) {
        // Make sure player has full health
        player.setHealth(player.getMaxHealth());
        
        // Set food level to maximum
        player.setFoodLevel(20);
        player.setSaturation(20f);
        
        // Make sure player is not on fire
        player.setFireTicks(0);
        
        // Clear any potion effects
        player.getActivePotionEffects().forEach(effect -> 
            player.removePotionEffect(effect.getType()));
            
        // Set game mode to adventure to prevent block breaking/placing
        player.setGameMode(GameMode.ADVENTURE);
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
        
        Game game = gameRegistry.getGame(gameName);
        if (game != null) {
            // Make sure player is unfrozen if they were a hunter
            if (headstartManager.isPlayerFrozen(playerId)) {
                headstartManager.unfreezeHunter(player);
            }
            
            game.removePlayer(player);
            gameRegistry.removePlayerFromGame(playerId);
            
            // Remove player from boss bar
            taskManager.removePlayerFromBossBar(gameName, player);
            
            // Update the lobby boss bar if in lobby state
            if (game.getState() == GameState.LOBBY) {
                taskManager.updateLobbyBossBar(game);
            }
        }
        
        // Restore player's state
        playerStateManager.restorePlayerState(player);
        
        logger.info("Removed player " + player.getName() + " from game " + gameName);
        return true;
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