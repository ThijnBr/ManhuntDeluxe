package com.thefallersgames.bettermanhunt.commands;

import com.thefallersgames.bettermanhunt.managers.GameManager;
import com.thefallersgames.bettermanhunt.managers.GuiManager;
import com.thefallersgames.bettermanhunt.models.Game;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Command handler for the /quitgame command.
 */
public class QuitGameCommand implements CommandExecutor {
    private final GameManager gameManager;
    private final GuiManager guiManager;
    
    /**
     * Creates a new quit game command handler.
     *
     * @param gameManager The game manager
     * @param guiManager The GUI manager
     */
    public QuitGameCommand(GameManager gameManager, GuiManager guiManager) {
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
        
        // Close any open GUI
        if (guiManager.getOpenInventoryType(player) != null) {
            player.closeInventory();
            guiManager.removeOpenInventory(player);
        }
        
        // Notify other players in the game
        String quitMessage = ChatColor.YELLOW + player.getName() + " has left the game.";
        for (Player p : player.getServer().getOnlinePlayers()) {
            if (game.isPlayerInGame(p) && !p.equals(player)) {
                p.sendMessage(quitMessage);
            }
        }
        
        // Remove player from the game
        gameManager.removePlayerFromGame(player);
        player.sendMessage(ChatColor.GREEN + "You have left the game.");
        
        return true;
    }
} 