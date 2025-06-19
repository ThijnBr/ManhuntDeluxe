package com.thefallersgames.bettermanhunt.commands;

import com.thefallersgames.bettermanhunt.managers.GameManager;
import com.thefallersgames.bettermanhunt.managers.GuiManager;
import com.thefallersgames.bettermanhunt.models.Game;
import com.thefallersgames.bettermanhunt.models.GameState;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Command handler for the /teamhunters command.
 */
public class TeamHuntersCommand implements CommandExecutor {
    private final GameManager gameManager;
    private final GuiManager guiManager;
    
    /**
     * Creates a new team hunters command handler.
     *
     * @param gameManager The game manager
     * @param guiManager The GUI manager
     */
    public TeamHuntersCommand(GameManager gameManager, GuiManager guiManager) {
        this.gameManager = gameManager;
        this.guiManager = guiManager;
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
        
        if (game.getState() != GameState.LOBBY) {
            player.sendMessage(ChatColor.RED + "You cannot change teams after the game has started.");
            return true;
        }
        
        boolean wasRunner = game.isRunner(player);
        boolean wasHunter = game.isHunter(player);
        
        if (game.addHunter(player)) {
            player.sendMessage(ChatColor.GREEN + "You joined the " + ChatColor.RED + "HUNTERS" + ChatColor.GREEN + " team.");
            
            // Show title to the player
            player.sendTitle(
                ChatColor.RED + "HUNTERS TEAM",
                ChatColor.GOLD + "Hunt down the runners!",
                10, 60, 20
            );
            
            if (wasRunner) {
                // Broadcast team change
                for (Player p : player.getServer().getOnlinePlayers()) {
                    if (game.isPlayerInGame(p) && !p.equals(player)) {
                        p.sendMessage(ChatColor.YELLOW + player.getName() + " switched from " + 
                                ChatColor.GREEN + "RUNNERS" + ChatColor.YELLOW + " to " + 
                                ChatColor.RED + "HUNTERS" + ChatColor.YELLOW + " team.");
                    }
                }
            } else if (!wasHunter) {
                // Broadcast team join
                for (Player p : player.getServer().getOnlinePlayers()) {
                    if (game.isPlayerInGame(p) && !p.equals(player)) {
                        p.sendMessage(ChatColor.YELLOW + player.getName() + " joined the " + 
                                ChatColor.RED + "HUNTERS" + ChatColor.YELLOW + " team.");
                    }
                }
            }
            
            // Update the lobby boss bar
            gameManager.updateLobbyBossBar(game);
            
            // Give lobby items to the player
            guiManager.giveLobbyItems(player, game);
        } else {
            player.sendMessage(ChatColor.YELLOW + "You are already on the " + 
                    ChatColor.RED + "HUNTERS" + ChatColor.YELLOW + " team.");
        }
        
        return true;
    }
} 