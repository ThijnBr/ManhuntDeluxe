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
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.World.Environment;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

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
                // This command is now for creating games with new worlds only
                // For current world, players should use /manhunt currentworld
                handleCreateCommand(player, args);
                break;
                
            case "currentworld":
                handleCurrentWorldCommand(player, args);
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
        // If Multiverse is not available, this command can only be used by admins
        // and it will create a game in the current world.
        if (!plugin.isMultiverseAvailable()) {
            if (player.hasPermission("bettermanhunt.admin")) {
                player.sendMessage(ChatColor.YELLOW + "Multiverse-Core not found. Creating a game in the current world instead...");
                handleCurrentWorldCommand(player, args);
            } else {
                player.sendMessage(ChatColor.RED + "This command requires Multiverse-Core to be installed to create new worlds.");
                player.sendMessage(ChatColor.YELLOW + "Please ask an admin to create a game for you in an existing world.");
            }
            return;
        }
        
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
     * Handles the /manhunt currentworld command.
     */
    private void handleCurrentWorldCommand(Player player, String[] args) {
        // Check if player has admin permission
        if (!player.hasPermission("bettermanhunt.admin")) {
            player.sendMessage(ChatColor.RED + "You don't have permission to create a game in the current world.");
            return;
        }
        
        // Check if player is already in a game
        Game currentGame = gameManager.getPlayerGame(player);
        if (currentGame != null) {
            player.sendMessage(ChatColor.RED + "You are already in a game. Leave it first with /quitgame.");
            return;
        }
        
        // Get the current world the player is in
        World playerWorld = player.getWorld();
        String worldName = playerWorld.getName();
        
        // Check if there's a game already running in this world
        for (Game game : gameManager.getAllGames()) {
            if (game.getWorld().getName().equals(worldName)) {
                player.sendMessage(ChatColor.RED + "A game is already running in this world. Please join that game or choose another world.");
                return;
            }
        }
        
        // Check and create Nether and End dimensions if they don't exist
        if (!ensureDimensionsExist(player, worldName)) {
            // ensureDimensionsExist will send error messages to the player
            return;
        }
        
        // Generate a game name based on the world
        String baseName = "game_" + worldName;
        String gameName = baseName;
        int counter = 1;
        
        // Make sure the game name is unique
        while (gameManager.getGame(gameName) != null) {
            gameName = baseName + "_" + counter++;
        }
        
        // Create the game with the current world
        boolean created = gameManager.createGame(gameName, player, playerWorld);
        
        if (created) {
            boolean teleportSuccess = lobbyService.teleportToLobbyCapsule(player, gameManager.getGame(gameName));
            player.sendMessage(ChatColor.GREEN + "Created new game in the current world with name: " + 
                    ChatColor.GOLD + gameName);
            
            // Join the game
            Game game = gameManager.getGame(gameName);
            if (game != null) {
                // Give player lobby items
                guiManager.giveLobbyItems(player, game);
                
                // Set up the player's state for the lobby
                plugin.getLobbyService().setupLobbyPlayerState(player);
            }
        } else {
            player.sendMessage(ChatColor.RED + "Failed to create game in the current world.");
        }
    }
    
    /**
     * Ensures that Nether and End dimensions exist for the given world.
     * Creates them if they don't exist.
     * 
     * @param player The player to notify of progress
     * @param worldName The base world name
     * @return true if dimensions exist or were created successfully, false otherwise
     */
    private boolean ensureDimensionsExist(Player player, String worldName) {
        // Check for Nether
        String netherWorldName = worldName + "_nether";
        World netherWorld = plugin.getServer().getWorld(netherWorldName);
        
        // Check for End
        String endWorldName = worldName + "_the_end";
        World endWorld = plugin.getServer().getWorld(endWorldName);
        
        // If Multiverse is not available, we can't create dimensions
        if (!plugin.isMultiverseAvailable()) {
            boolean hasBothDimensions = (netherWorld != null && endWorld != null);
            
            if (!hasBothDimensions) {
                if (netherWorld == null) {
                    player.sendMessage(ChatColor.RED + "The Nether dimension is missing and Multiverse-Core is not installed to create it.");
                }
                
                if (endWorld == null) {
                    player.sendMessage(ChatColor.RED + "The End dimension is missing and Multiverse-Core is not installed to create it.");
                }
                
                player.sendMessage(ChatColor.RED + "You need both dimensions to play Manhunt. Please install Multiverse-Core or create these dimensions manually.");
                return false;
            }
            
            return true;
        }
        
        // Create missing dimensions using Multiverse
        if (netherWorld == null) {
            player.sendMessage(ChatColor.YELLOW + "Creating Nether dimension for this world...");
            
            WorldCreator netherCreator = new WorldCreator(netherWorldName);
            netherCreator.environment(Environment.NETHER);
            netherCreator.seed(plugin.getServer().getWorld(worldName).getSeed());
            netherCreator.generateStructures(true);
            
            netherWorld = netherCreator.createWorld();
            player.sendMessage(ChatColor.GREEN + "Nether dimension created successfully.");
        }
        
        if (endWorld == null) {
            player.sendMessage(ChatColor.YELLOW + "Creating End dimension for this world...");
            
            WorldCreator endCreator = new WorldCreator(endWorldName);
            endCreator.environment(Environment.THE_END);
            endCreator.seed(plugin.getServer().getWorld(worldName).getSeed());
            endCreator.generateStructures(true);
            
            endWorld = endCreator.createWorld();
            player.sendMessage(ChatColor.GREEN + "End dimension created successfully.");
        }
        
        return true;
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
                player.sendMessage(ChatColor.YELLOW + "Starting game...");
                gameManager.startGame(game).thenAccept(started -> {
                    if (!started) {
                        player.sendMessage(ChatColor.RED + "Failed to start game. Make sure there is at least one hunter and one runner.");
                    }
                });
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
        
        // Notify player that the game is starting
        player.sendMessage(ChatColor.YELLOW + "Starting game " + ChatColor.GOLD + gameName + ChatColor.YELLOW + "...");
        
        // Broadcast to all players in the game
        for (Player p : plugin.getServer().getOnlinePlayers()) {
            if (game.isPlayerInGame(p) && !p.equals(player)) {
                p.sendMessage(ChatColor.YELLOW + "Game " + ChatColor.GOLD + gameName + 
                        ChatColor.YELLOW + " is starting...");
            }
        }
        
        // Start the game with async completion
        gameManager.startGame(game).thenAccept(started -> {
            if (started) {
                // Send success message to all players in the game
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
                            if (game.getState() == GameState.HEADSTART) {
                                p.sendMessage(ChatColor.YELLOW + "You are frozen during the headstart period.");
                            }
                        }
                    }
                }
            } else {
                player.sendMessage(ChatColor.RED + "Failed to start game. Make sure there is at least one hunter and one runner.");
            }
        }).exceptionally(ex -> {
            player.sendMessage(ChatColor.RED + "An error occurred while starting the game: " + ex.getMessage());
            plugin.getLogger().severe("Error starting game: " + ex.getMessage());
            ex.printStackTrace();
            return null;
        });
    }
    
    /**
     * Handles the /manhunt list command.
     */
    private void handleListCommand(Player player) {
        // Combine lobby and active games
        List<Game> lobbyGames = gameManager.getLobbyGames();
        List<Game> activeGames = gameManager.getActiveGames();
        List<Game> allGames = new ArrayList<>();
        allGames.addAll(lobbyGames);
        allGames.addAll(activeGames);
        
        if (allGames.isEmpty()) {
            player.sendMessage(ChatColor.YELLOW + "There are no active games. Create one with /manhunt create.");
            return;
        }
        
        player.sendMessage(ChatColor.GREEN + "===== Manhunt Games =====");
        for (Game game : allGames) {
            String status;
            if (game.getState() == GameState.LOBBY) {
                status = ChatColor.GREEN + "LOBBY";
            } else if (game.getState() == GameState.HEADSTART) {
                status = ChatColor.GOLD + "HEADSTART";
            } else {
                status = ChatColor.RED + "ACTIVE";
            }
            
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
        player.sendMessage(ChatColor.YELLOW + "/manhunt create" + ChatColor.WHITE + " - Create a new manhunt game with a new world (requires Multiverse-Core)");
        player.sendMessage(ChatColor.YELLOW + "/manhunt join [name]" + ChatColor.WHITE + " - Join a manhunt game");
        player.sendMessage(ChatColor.YELLOW + "/manhunt list" + ChatColor.WHITE + " - List all manhunt games");
        player.sendMessage(ChatColor.YELLOW + "/manhunt lobby" + ChatColor.WHITE + " - Teleport to the global lobby");
        player.sendMessage(ChatColor.YELLOW + "/teamhunters" + ChatColor.WHITE + " - Join the hunters team");
        player.sendMessage(ChatColor.YELLOW + "/teamrunners" + ChatColor.WHITE + " - Join the runners team");
        player.sendMessage(ChatColor.YELLOW + "/quitgame" + ChatColor.WHITE + " - Leave your current manhunt game");
        
        player.sendMessage(ChatColor.GOLD + "=== Game Owner Commands ===");
        player.sendMessage(ChatColor.YELLOW + "/manhunt delete <name>" + ChatColor.WHITE + " - Delete your manhunt game");
        player.sendMessage(ChatColor.YELLOW + "/manhunt start [name]" + ChatColor.WHITE + " - Start your manhunt game");
        
        if (player.hasPermission("bettermanhunt.admin")) {
            player.sendMessage(ChatColor.GOLD + "=== Admin Commands ===");
            player.sendMessage(ChatColor.YELLOW + "/manhunt setlobby" + ChatColor.WHITE + " - Set the main lobby location");
            player.sendMessage(ChatColor.YELLOW + "/manhunt currentworld" + ChatColor.WHITE + " - Create a new manhunt game in the current world");
            player.sendMessage(ChatColor.YELLOW + "/manhunt delete <name>" + ChatColor.WHITE + " - Delete any manhunt game");
            player.sendMessage(ChatColor.YELLOW + "/manhunt start <name>" + ChatColor.WHITE + " - Start any manhunt game");
        }
    }
} 