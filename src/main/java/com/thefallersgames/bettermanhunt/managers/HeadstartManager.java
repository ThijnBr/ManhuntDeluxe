package com.thefallersgames.bettermanhunt.managers;

import com.thefallersgames.bettermanhunt.Plugin;
import com.thefallersgames.bettermanhunt.models.Game;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages the headstart period for hunters, keeping them frozen at spawn.
 */
public class HeadstartManager {
    private final Plugin plugin;
    private final Map<UUID, Location> frozenHunterLocations = new ConcurrentHashMap<>();

    /**
     * Constructs a new HeadstartManager.
     *
     * @param plugin The plugin instance
     */
    public HeadstartManager(Plugin plugin) {
        this.plugin = plugin;
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