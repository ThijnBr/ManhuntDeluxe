package com.thefallersgames.bettermanhunt.managers;

import com.thefallersgames.bettermanhunt.Plugin;
import com.thefallersgames.bettermanhunt.models.Game;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Manages team chat functionality for players in Manhunt games.
 */
public class TeamChatManager {
    private final Plugin plugin;
    private final Map<UUID, Boolean> teamChatEnabled;

    /**
     * Creates a new TeamChatManager.
     *
     * @param plugin The plugin instance
     */
    public TeamChatManager(Plugin plugin) {
        this.plugin = plugin;
        this.teamChatEnabled = new HashMap<>();
    }

    /**
     * Handles chat events for team chat.
     *
     * @param event The chat event
     */
    public void handleChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        Game game = plugin.getGameManager().getPlayerGame(player);
        
        if (game == null || !isTeamChatEnabled(player)) {
            // Not in a game or team chat disabled, leave default chat behavior
            return;
        }
        
        // Team chat logic
        event.setCancelled(true);
        String teamPrefix;
        String message = event.getMessage();
        
        if (game.isHunter(player)) {
            teamPrefix = ChatColor.RED + "[HUNTERS] ";
            sendTeamMessage(game.getHunters(), teamPrefix, player, message);
        } else if (game.isRunner(player)) {
            teamPrefix = ChatColor.GREEN + "[RUNNERS] ";
            sendTeamMessage(game.getRunners(), teamPrefix, player, message);
        } else {
            teamPrefix = ChatColor.GRAY + "[SPECTATORS] ";
            sendTeamMessage(game.getSpectators(), teamPrefix, player, message);
        }
    }
    
    /**
     * Helper method to send a message to team members
     */
    private void sendTeamMessage(Iterable<UUID> teamMembers, String prefix, Player sender, String message) {
        for (UUID memberId : teamMembers) {
            Player member = plugin.getServer().getPlayer(memberId);
            if (member != null) {
                member.sendMessage(prefix + ChatColor.RESET + sender.getName() + ": " + message);
            }
        }
    }

    /**
     * Toggles team chat for a player.
     *
     * @param player The player to toggle team chat for
     * @return True if team chat is now enabled, false if disabled
     */
    public boolean toggleTeamChat(Player player) {
        UUID playerId = player.getUniqueId();
        boolean enabled = !isTeamChatEnabled(player);
        teamChatEnabled.put(playerId, enabled);
        return enabled;
    }

    /**
     * Checks if team chat is enabled for a player.
     *
     * @param player The player to check
     * @return True if team chat is enabled, false otherwise
     */
    public boolean isTeamChatEnabled(Player player) {
        return teamChatEnabled.getOrDefault(player.getUniqueId(), false);
    }
    
    /**
     * Removes a player from team chat tracking.
     * 
     * @param player The player to remove
     */
    public void removePlayer(Player player) {
        teamChatEnabled.remove(player.getUniqueId());
    }
} 