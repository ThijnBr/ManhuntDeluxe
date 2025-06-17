package com.thefallersgames.bettermanhunt.listeners;

import com.thefallersgames.bettermanhunt.Plugin;
import com.thefallersgames.bettermanhunt.managers.HeadstartManager;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

/**
 * Listener for handling frozen hunter movement during headstart period.
 */
public class HeadstartListener implements Listener {
    private final HeadstartManager headstartManager;
    
    /**
     * Constructs a new HeadstartListener.
     *
     * @param plugin The plugin instance
     * @param headstartManager The headstart manager to use
     */
    public HeadstartListener(Plugin plugin, HeadstartManager headstartManager) {
        this.headstartManager = headstartManager;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }
    
    /**
     * Teleports frozen hunters back to their spawn position if they attempt to move.
     */
    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        
        if (headstartManager.isPlayerFrozen(player.getUniqueId())) {
            // Checking if player actually moved position (not just looking around)
            Location from = event.getFrom();
            Location to = event.getTo();
            
            if (to != null && (from.getX() != to.getX() || from.getY() != to.getY() || from.getZ() != to.getZ())) {
                // Player moved position, teleport them back
                player.teleport(headstartManager.getFrozenLocation(player.getUniqueId()));
            }
        }
    }
} 