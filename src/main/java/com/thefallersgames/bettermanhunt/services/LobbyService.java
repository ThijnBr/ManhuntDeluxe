package com.thefallersgames.bettermanhunt.services;

import com.thefallersgames.bettermanhunt.Plugin;
import com.thefallersgames.bettermanhunt.models.Game;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.configuration.file.FileConfiguration;

/**
 * Service responsible for handling all lobby-related functionality.
 */
public class LobbyService {
    private final Plugin plugin;
    private Location lobbySpawn;
    
    /**
     * Creates a new LobbyService.
     *
     * @param plugin The plugin instance
     */
    public LobbyService(Plugin plugin) {
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
            World world = game.getWorld();
            Location worldSpawn = world.getSpawnLocation().clone();
            
            // Create capsule 100 blocks above world spawn
            Location capsuleCenter = worldSpawn.clone().add(0, 100, 0);
            
            // Check if capsule already exists, if not, create it
            if (!isCapsulePresent(capsuleCenter)) {
                createGlassCapsule(capsuleCenter);
            }
            
            // Teleport player inside the capsule
            Location teleportLocation = capsuleCenter.clone().add(0, 1, 0);
            return player.teleport(teleportLocation);
        } catch (Exception e) {
            plugin.getLogger().warning("Error teleporting player to lobby capsule: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Checks if a glass capsule is already present at the given location.
     *
     * @param center The center location of the capsule
     * @return True if the capsule exists
     */
    private boolean isCapsulePresent(Location center) {
        // Simple check: just check the floor block
        return center.clone().subtract(0, 1, 0).getBlock().getType() == Material.GLASS;
    }
    
    /**
     * Creates a glass capsule around the given center location.
     *
     * @param center The center location for the capsule
     */
    private void createGlassCapsule(Location center) {        
        // Create the capsule
        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                for (int y = -1; y <= 2; y++) {
                    // Skip the center blocks to make an open space
                    if (y > -1 && y < 2 && Math.abs(x) < 2 && Math.abs(z) < 2) {
                        continue;
                    }
                    
                    Location blockLoc = center.clone().add(x, y, z);
                    blockLoc.getBlock().setType(Material.GLASS);
                }
            }
        }
        
        // Add some light
        center.clone().add(0, 2, 0).getBlock().setType(Material.GLOWSTONE);
    }
    
    /**
     * Sets up a player's state for the lobby.
     *
     * @param player The player to set up
     */
    public void setupLobbyPlayerState(Player player) {
        // Make sure player has full health
        double maxHealth = player.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue();
        player.setHealth(maxHealth);
        
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
} 