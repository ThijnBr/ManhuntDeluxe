package com.thefallersgames.bettermanhunt.commands;

import com.thefallersgames.bettermanhunt.listeners.PlayerListener;
import com.thefallersgames.bettermanhunt.managers.GameManager;
import com.thefallersgames.bettermanhunt.models.Game;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Command handlers for the chat toggle commands (/toall, /toteam).
 */
public class ChatToggleCommands {
    
    /**
     * Command handler for /toall command.
     */
    public static class ToAllCommand implements CommandExecutor {
        private final PlayerListener playerListener;
        private final GameManager gameManager;
        
        /**
         * Creates a new to all command handler.
         *
         * @param playerListener The player listener
         * @param gameManager The game manager
         */
        public ToAllCommand(PlayerListener playerListener, GameManager gameManager) {
            this.playerListener = playerListener;
            this.gameManager = gameManager;
        }
        
        @Override
        public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(ChatColor.RED + "This command can only be used by players.");
                return true;
            }
            
            Player player = (Player) sender;
            Game game = gameManager.getPlayerGame(player);
            
            if (game == null) {
                player.sendMessage(ChatColor.RED + "You are not in a manhunt game.");
                return true;
            }
            
            // Toggle team chat off (force to global chat)
            if (playerListener.isTeamChatEnabled(player)) {
                playerListener.toggleTeamChat(player); // Turn off team chat
                player.sendMessage(ChatColor.GREEN + "Chat set to " + ChatColor.GOLD + "GLOBAL" + 
                        ChatColor.GREEN + ". All players will see your messages.");
            } else {
                player.sendMessage(ChatColor.YELLOW + "You are already in global chat mode.");
            }
            
            return true;
        }
    }
    
    /**
     * Command handler for /toteam command.
     */
    public static class ToTeamCommand implements CommandExecutor {
        private final PlayerListener playerListener;
        private final GameManager gameManager;
        
        /**
         * Creates a new to team command handler.
         *
         * @param playerListener The player listener
         * @param gameManager The game manager
         */
        public ToTeamCommand(PlayerListener playerListener, GameManager gameManager) {
            this.playerListener = playerListener;
            this.gameManager = gameManager;
        }
        
        @Override
        public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(ChatColor.RED + "This command can only be used by players.");
                return true;
            }
            
            Player player = (Player) sender;
            Game game = gameManager.getPlayerGame(player);
            
            if (game == null) {
                player.sendMessage(ChatColor.RED + "You are not in a manhunt game.");
                return true;
            }
            
            // Toggle team chat
            boolean teamChatEnabled = playerListener.toggleTeamChat(player);
            
            if (teamChatEnabled) {
                String team;
                if (game.isHunter(player)) {
                    team = ChatColor.RED + "HUNTERS";
                } else if (game.isRunner(player)) {
                    team = ChatColor.GREEN + "RUNNERS";
                } else {
                    team = ChatColor.GRAY + "SPECTATORS";
                }
                
                player.sendMessage(ChatColor.GREEN + "Chat set to " + ChatColor.GOLD + "TEAM" + 
                        ChatColor.GREEN + ". Only " + team + ChatColor.GREEN + " will see your messages.");
            } else {
                player.sendMessage(ChatColor.GREEN + "Chat set to " + ChatColor.GOLD + "GLOBAL" + 
                        ChatColor.GREEN + ". All players will see your messages.");
            }
            
            return true;
        }
    }
} 