package com.thefallersgames.bettermanhunt.managers;

import com.thefallersgames.bettermanhunt.Plugin;
import com.thefallersgames.bettermanhunt.services.LobbyService;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Manages player state, including inventory, game mode, world, and position.
 */
public class PlayerStateManager {
    private final Map<UUID, PlayerState> playerStates;
    private final LobbyService lobbyService;
    
    /**
     * Creates a new PlayerStateManager.
     * 
     * @param plugin The plugin instance
     */
    public PlayerStateManager(Plugin plugin) {
        this.playerStates = new HashMap<>();
        this.lobbyService = plugin.getLobbyService();
    }
    
    /**
     * Saves the player's current state.
     *
     * @param player The player
     */
    public void savePlayerState(Player player) {
        PlayerState state = new PlayerState(
                player.getInventory().getContents(),
                player.getGameMode(),
                player.getLocation()
        );
        playerStates.put(player.getUniqueId(), state);
        player.getInventory().clear();
    }
    
    /**
     * Restores the player's state.
     * Returns the player to the global lobby if set, or to their original location if not.
     *
     * @param player The player
     */
    public void restorePlayerState(Player player) {
        PlayerState state = playerStates.remove(player.getUniqueId());
        if (state != null) {
            player.setGameMode(state.getGameMode());
            
            // Check if the global lobby spawn is set, if so use that instead of original location
            Location targetLocation;
            Location lobbySpawn = lobbyService.getLobbySpawn();
            if (lobbySpawn != null) {
                targetLocation = lobbySpawn;
                player.sendMessage("§aYou have been teleported to the lobby.");
            } else {
                targetLocation = state.getLocation();
                player.sendMessage("§aYou have been returned to your previous location.");
            }
            
            player.teleport(targetLocation);
            player.getInventory().setContents(state.getInventory());
        } else {
            player.setGameMode(GameMode.SURVIVAL);
            player.getInventory().clear();
            
            // Try to teleport to lobby if it exists
            Location lobbySpawn = lobbyService.getLobbySpawn();
            if (lobbySpawn != null) {
                player.teleport(lobbySpawn);
                player.sendMessage("§aYou have been teleported to the lobby.");
            }
        }
    }
    
    /**
     * Class to hold a player's state.
     */
    private static class PlayerState {
        private final ItemStack[] inventory;
        private final GameMode gameMode;
        private final Location location;
        
        public PlayerState(ItemStack[] inventory, GameMode gameMode, Location location) {
            this.inventory = inventory;
            this.gameMode = gameMode;
            this.location = location;
        }
        
        public ItemStack[] getInventory() {
            return inventory;
        }
        
        public GameMode getGameMode() {
            return gameMode;
        }
        
        public Location getLocation() {
            return location;
        }
    }
} 