package com.thefallersgames.bettermanhunt.managers;

import com.thefallersgames.bettermanhunt.Plugin;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

/**
 * Manages the global lobby spawn point.
 */
public class LobbyManager {
    private final Plugin plugin;
    private Location lobbySpawn;
    
    /**
     * Creates a new LobbyManager.
     *
     * @param plugin The plugin instance
     */
    public LobbyManager(Plugin plugin) {
        this.plugin = plugin;
        loadLobbySpawn();
    }
    
    /**
     * Sets the lobby spawn point to the player's current location.
     *
     * @param player The player whose location to use
     * @return True if the lobby spawn was set, false if there was an error
     */
    public boolean setLobbySpawn(Player player) {
        // Save the player's current location as the lobby spawn
        lobbySpawn = player.getLocation().clone();
        
        // Save to config
        FileConfiguration config = plugin.getConfig();
        config.set("lobby.world", lobbySpawn.getWorld().getName());
        config.set("lobby.x", lobbySpawn.getX());
        config.set("lobby.y", lobbySpawn.getY());
        config.set("lobby.z", lobbySpawn.getZ());
        config.set("lobby.yaw", lobbySpawn.getYaw());
        config.set("lobby.pitch", lobbySpawn.getPitch());
        
        plugin.saveConfig();
        return true;
    }
    
    /**
     * Gets the lobby spawn location.
     *
     * @return The lobby spawn location, or null if none is set
     */
    public Location getLobbySpawn() {
        return lobbySpawn;
    }
    
    /**
     * Teleports a player to the lobby spawn.
     *
     * @param player The player to teleport
     * @return True if the player was teleported, false if there is no lobby spawn set
     */
    public boolean teleportToLobby(Player player) {
        if (lobbySpawn == null) {
            return false;
        }
        
        player.teleport(lobbySpawn);
        return true;
    }
    
    /**
     * Loads the lobby spawn point from the config.
     */
    private void loadLobbySpawn() {
        FileConfiguration config = plugin.getConfig();
        if (config.contains("lobby.world")) {
            String worldName = config.getString("lobby.world");
            double x = config.getDouble("lobby.x");
            double y = config.getDouble("lobby.y");
            double z = config.getDouble("lobby.z");
            float yaw = (float) config.getDouble("lobby.yaw");
            float pitch = (float) config.getDouble("lobby.pitch");
            
            if (plugin.getServer().getWorld(worldName) != null) {
                lobbySpawn = new Location(plugin.getServer().getWorld(worldName), x, y, z, yaw, pitch);
                plugin.getLogger().info("Loaded lobby spawn at: " + worldName + ", " + x + ", " + y + ", " + z);
            } else {
                plugin.getLogger().warning("Could not load lobby spawn: world " + worldName + " does not exist");
            }
        } else {
            plugin.getLogger().info("No lobby spawn set. Use /manhunt setlobby to set one.");
        }
    }
} 