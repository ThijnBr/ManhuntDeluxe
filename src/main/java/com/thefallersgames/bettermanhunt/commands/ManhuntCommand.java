package com.thefallersgames.bettermanhunt.commands;

import com.thefallersgames.bettermanhunt.Plugin;
import com.thefallersgames.bettermanhunt.managers.GameManager;
import com.thefallersgames.bettermanhunt.managers.GuiManager;
import com.thefallersgames.bettermanhunt.models.Game;
import com.thefallersgames.bettermanhunt.models.GameState;
import com.thefallersgames.bettermanhunt.services.LobbyService;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * Command handler for the main /manhunt command.
 */
public class ManhuntCommand implements CommandExecutor {
    private final Plugin plugin;
    private final GameManager gameManager;
    private final GuiManager guiManager;
    private final LobbyService lobbyService;
    
    /**
     * Creates a new manhunt command handler.
     *
     * @param plugin The plugin instance
     * @param gameManager The game manager
     */
    public ManhuntCommand(Plugin plugin, GameManager gameManager, GuiManager guiManager) {
        this.plugin = plugin;
        this.gameManager = gameManager;
        this.guiManager = guiManager;
        this.lobbyService = plugin.getLobbyService();
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "This command can only be used by players.");
            return true;
        }
        
        Player player = (Player) sender;
        
        if (args.length == 0) {
            sendHelpMessage(player);
            return true;
        }
        
        String subCommand = args[0].toLowerCase();
        
        switch (subCommand) {
            case "create":
                handleCreateCommand(player, args);
                break;
                
            case "delete":
                handleDeleteCommand(player, args);
                break;
                
            case "join":
                handleJoinCommand(player, args);
                break;
                
            case "start":
                handleStartCommand(player, args);
                break;
                
            case "list":
                handleListCommand(player);
                break;
                
            case "setlobby":
                handleSetLobbyCommand(player);
                break;
                
            case "lobby":
                handleLobbyCommand(player);
                break;
                
                
            default:
                sendHelpMessage(player);
                break;
        }
        
        return true;
    }
    
    /**
     * Handles the /manhunt create command.
     */
    private void handleCreateCommand(Player player, String[] args) {
        // Check if player is already in a game
        Game currentGame = gameManager.getPlayerGame(player);
        if (currentGame != null) {
            player.sendMessage(ChatColor.RED + "You are already in a game. Leave it first with /quitgame.");
            return;
        }
        
        // Show world selection GUI
        guiManager.showWorldSelectionGui(player);
    }
    
    /**
     * Handles the /manhunt delete command.
     */
    private void handleDeleteCommand(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "Usage: /manhunt delete <game-name>");
            return;
        }
        
        String gameName = args[1];
        Game game = gameManager.getGame(gameName);
        
        if (game == null) {
            player.sendMessage(ChatColor.RED + "No game found with name: " + gameName);
            return;
        }
        
        // Check if player is the owner or has admin permission
        if (!game.isOwner(player) && !player.hasPermission("bettermanhunt.admin")) {
            player.sendMessage(ChatColor.RED + "Only the game owner or admins can delete a game.");
            return;
        }
        
        // Delete the game
        boolean deleted = gameManager.deleteGame(gameName);
        if (deleted) {
            player.sendMessage(ChatColor.GREEN + "Deleted game: " + ChatColor.GOLD + gameName);
        } else {
            player.sendMessage(ChatColor.RED + "Failed to delete game: " + gameName);
        }
    }
    
    /**
     * Handles the /manhunt join command.
     */
    private void handleJoinCommand(Player player, String[] args) {
        // Check if player is already in a game
        Game currentGame = gameManager.getPlayerGame(player);
        if (currentGame != null) {
            player.sendMessage(ChatColor.RED + "You are already in a game. Leave it first with /quitgame.");
            return;
        }
        
        // If a game name is provided, try to join that specific game
        if (args.length >= 2) {
            String gameName = args[1];
            Game game = gameManager.getGame(gameName);
            
            if (game == null) {
                player.sendMessage(ChatColor.RED + "No game found with name: " + gameName);
                return;
            }
            
            // Check if the game is in LOBBY state
            if (game.getState() != GameState.LOBBY) {
                player.sendMessage(ChatColor.RED + "Cannot join game: The game has already started.");
                return;
            }
            
            // Add player to the game
            boolean joined = gameManager.addPlayerToGame(player, game);
            if (joined) {
                player.sendMessage(ChatColor.GREEN + "Joined game: " + ChatColor.GOLD + gameName);
                
                // Lobby items are given in addPlayerToGame when in lobby state
                // No need to give them again here
            } else {
                player.sendMessage(ChatColor.RED + "Failed to join game: " + gameName);
            }
        } else {
            // Show game selection GUI
            guiManager.showGameSelectionGui(player);
        }
    }
    
    /**
     * Handles the /manhunt start command.
     */
    private void handleStartCommand(Player player, String[] args) {
        if (args.length < 2) {
            // Check if player is in a game and is the owner
            Game game = gameManager.getPlayerGame(player);
            if (game != null && game.isOwner(player)) {
                // Start the game
                boolean started = gameManager.startGame(game);
                if (!started) {
                    player.sendMessage(ChatColor.RED + "Failed to start game. Make sure there is at least one hunter and one runner.");
                }
                return;
            }
            
            player.sendMessage(ChatColor.RED + "Usage: /manhunt start <game-name>");
            return;
        }
        
        String gameName = args[1];
        Game game = gameManager.getGame(gameName);
        
        if (game == null) {
            player.sendMessage(ChatColor.RED + "No game found with name: " + gameName);
            return;
        }
        
        // Check if player is the owner or has admin permission
        if (!game.isOwner(player) && !player.hasPermission("bettermanhunt.admin")) {
            player.sendMessage(ChatColor.RED + "Only the game owner or admins can start a game.");
            return;
        }
        
        // Check if the game is in LOBBY state
        if (game.getState() != GameState.LOBBY) {
            player.sendMessage(ChatColor.RED + "The game has already started.");
            return;
        }
        
        // Start the game
        boolean started = gameManager.startGame(game);
        if (started) {
            // Send message to all players in the game
            for (Player p : plugin.getServer().getOnlinePlayers()) {
                if (game.isPlayerInGame(p)) {
                    p.sendMessage(ChatColor.GREEN + "Game " + ChatColor.GOLD + gameName + 
                            ChatColor.GREEN + " has started!");
                    
                    if (game.isRunner(p)) {
                        p.sendMessage(ChatColor.GREEN + "You are a " + ChatColor.GOLD + "RUNNER" + 
                                ChatColor.GREEN + ". Survive and defeat the Ender Dragon to win!");
                    } else if (game.isHunter(p)) {
                        p.sendMessage(ChatColor.RED + "You are a " + ChatColor.GOLD + "HUNTER" + 
                                ChatColor.RED + ". Kill all runners before they defeat the Ender Dragon!");
                        p.sendMessage(ChatColor.YELLOW + "You will be frozen during the headstart period.");
                    }
                }
            }
        } else {
            player.sendMessage(ChatColor.RED + "Failed to start game. Make sure there is at least one hunter and one runner.");
        }
    }
    
    /**
     * Handles the /manhunt list command.
     */
    private void handleListCommand(Player player) {
        List<Game> activeGames = gameManager.getActiveOrLobbyGames();
        
        if (activeGames.isEmpty()) {
            player.sendMessage(ChatColor.YELLOW + "There are no active games. Create one with /manhunt create.");
            return;
        }
        
        player.sendMessage(ChatColor.GREEN + "===== Manhunt Games =====");
        for (Game game : activeGames) {
            String status = game.getState() == GameState.LOBBY ? ChatColor.GREEN + "LOBBY" : ChatColor.RED + "ACTIVE";
            player.sendMessage(ChatColor.GOLD + game.getName() + ChatColor.GRAY + " - " + status + 
                    ChatColor.GRAY + " - Runners: " + ChatColor.YELLOW + game.getRunners().size() + 
                    ChatColor.GRAY + " - Hunters: " + ChatColor.YELLOW + game.getHunters().size());
        }
        player.sendMessage(ChatColor.GREEN + "======================");
        player.sendMessage(ChatColor.YELLOW + "To join a game: /manhunt join <name>");
    }
    
    /**
     * Handles the /manhunt setlobby command.
     */
    private void handleSetLobbyCommand(Player player) {
        // Check if player has permission
        if (!player.hasPermission("bettermanhunt.admin")) {
            player.sendMessage(ChatColor.RED + "You don't have permission to set the lobby location.");
            return;
        }
        
        // Set the lobby location to the player's current position
        boolean success = lobbyService.setLobbySpawn(player);
        
        if (success) {
            player.sendMessage(ChatColor.GREEN + "Lobby spawn point has been set to your current location.");
        } else {
            player.sendMessage(ChatColor.RED + "Failed to set lobby spawn point.");
        }
    }
    
    /**
     * Handles the /manhunt lobby command.
     */
    private void handleLobbyCommand(Player player) {
        // Check if player is in a game
        Game currentGame = gameManager.getPlayerGame(player);
        if (currentGame != null) {
            player.sendMessage(ChatColor.RED + "You are in a game. Use /quitgame first to leave the game.");
            return;
        }

        
        // Teleport player to the lobby
        boolean teleported = lobbyService.teleportToLobby(player);
        
        if (teleported) {
            player.sendMessage(ChatColor.GREEN + "You have been teleported to the lobby.");
        } else {
            player.sendMessage(ChatColor.RED + "No lobby spawn has been set. An admin must use /manhunt setlobby first.");
        }
    }

    /**
     * Sends the help message to a player.
     *
     * @param player The player to send the help message to
     */
    private void sendHelpMessage(Player player) {
        player.sendMessage(ChatColor.GOLD + "=== BetterManhunt Commands ===");
        player.sendMessage(ChatColor.YELLOW + "/manhunt create" + ChatColor.WHITE + " - Create a new manhunt game");
        player.sendMessage(ChatColor.YELLOW + "/manhunt delete <name>" + ChatColor.WHITE + " - Delete a manhunt game");
        player.sendMessage(ChatColor.YELLOW + "/manhunt join [name]" + ChatColor.WHITE + " - Join a manhunt game");
        player.sendMessage(ChatColor.YELLOW + "/manhunt start [name]" + ChatColor.WHITE + " - Start a manhunt game");
        player.sendMessage(ChatColor.YELLOW + "/manhunt list" + ChatColor.WHITE + " - List all manhunt games");
        player.sendMessage(ChatColor.YELLOW + "/manhunt lobby" + ChatColor.WHITE + " - Teleport to the main lobby");
        player.sendMessage(ChatColor.YELLOW + "/teamhunters" + ChatColor.WHITE + " - Join the hunters team");
        player.sendMessage(ChatColor.YELLOW + "/teamrunners" + ChatColor.WHITE + " - Join the runners team");
        player.sendMessage(ChatColor.YELLOW + "/quitgame" + ChatColor.WHITE + " - Leave your current manhunt game");
        
        if (player.hasPermission("bettermanhunt.admin")) {
            player.sendMessage(ChatColor.GOLD + "=== Admin Commands ===");
            player.sendMessage(ChatColor.YELLOW + "/manhunt setlobby" + ChatColor.WHITE + " - Set the main lobby location");
            player.sendMessage(ChatColor.YELLOW + "/manhunt edit <world>" + ChatColor.WHITE + " - Edit a world in creative mode");
        }
    }
} 