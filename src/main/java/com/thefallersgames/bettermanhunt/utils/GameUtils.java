package com.thefallersgames.bettermanhunt.utils;

import com.thefallersgames.bettermanhunt.Plugin;
import com.thefallersgames.bettermanhunt.models.Game;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

/**
 * Utility methods for Manhunt games.
 */
public class GameUtils {
    
    /**
     * Broadcasts a message to all players in a game.
     *
     * @param plugin The plugin instance
     * @param game The game to broadcast to
     * @param message The message to broadcast
     */
    public static void broadcastMessageToGame(Plugin plugin, Game game, String message) {
        for (UUID playerId : game.getAllPlayers()) {
            Player player = plugin.getServer().getPlayer(playerId);
            if (player != null) {
                player.sendMessage(message);
            }
        }
    }
    
    /**
     * Checks if a player has a compass in their inventory.
     *
     * @param player The player to check
     * @return True if the player has a compass, false otherwise
     */
    public static boolean hasCompass(Player player) {
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() == Material.COMPASS) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Formats a game state message with appropriate color.
     *
     * @param game The game
     * @return A colored string representing the game state
     */
    public static String formatGameState(Game game) {
        switch (game.getState()) {
            case LOBBY:
                return ChatColor.GREEN + "LOBBY";
            case STARTING:
                return ChatColor.YELLOW + "STARTING";
            case TELEPORTING:
                return ChatColor.YELLOW + "TELEPORTING";
            case HEADSTART:
                return ChatColor.GOLD + "HEADSTART";
            case ACTIVE:
                return ChatColor.RED + "ACTIVE";
            case RUNNERS_WON:
                return ChatColor.BLUE + "RUNNERS WON";
            case HUNTERS_WON:
                return ChatColor.RED + "HUNTERS WON";
            case ENDING:
                return ChatColor.GRAY + "ENDING";
            case DELETING:
                return ChatColor.GRAY + "DELETING";
            default:
                return ChatColor.WHITE + game.getState().toString();
        }
    }
} 