package com.thefallersgames.bettermanhunt.listeners;

import com.thefallersgames.bettermanhunt.Plugin;
import com.thefallersgames.bettermanhunt.managers.GameManager;
import com.thefallersgames.bettermanhunt.managers.GuiManager;
import com.thefallersgames.bettermanhunt.models.Game;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Random;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Handles GUI interactions and special item usage.
 */
public class GuiListener implements Listener {
    private final Plugin plugin;
    private final GameManager gameManager;
    private final GuiManager guiManager;
    
    private final Random random = new Random();
    
    /**
     * Creates a new GUI listener.
     *
     * @param plugin The plugin instance
     * @param gameManager The game manager
     * @param guiManager The GUI manager
     */
    public GuiListener(Plugin plugin, GameManager gameManager, GuiManager guiManager) {
        this.plugin = plugin;
        this.gameManager = gameManager;
        this.guiManager = guiManager;
        
        // Register events
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }
    
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }
        
        Player player = (Player) event.getWhoClicked();
        ItemStack clickedItem = event.getCurrentItem();
        
        // Cancel all clicks in GUIs
        if (guiManager.getOpenInventoryType(player) != null) {
            event.setCancelled(true);
            
            // Check if the clicked item is valid
            if (clickedItem == null || clickedItem.getType() == Material.AIR || !clickedItem.hasItemMeta()) {
                return;
            }
            
            // Handle clicks based on inventory type
            String inventoryType = guiManager.getOpenInventoryType(player);
            
            switch (inventoryType) {
                case GuiManager.GAME_SELECTION_GUI:
                    handleGameSelectionClick(player, clickedItem);
                    break;
                    
                case GuiManager.WORLD_SELECTION_GUI:
                    handleWorldSelectionClick(player, clickedItem);
                    break;
                    
                case GuiManager.TEAM_SELECTION_GUI:
                    handleTeamSelectionClick(player, clickedItem);
                    break;
            }
        }
        
        // Prevent moving lobby items
        Game playerGame = gameManager.getPlayerGame(player);
        if (playerGame != null && playerGame.getState().toString().startsWith("LOBBY")) {
            event.setCancelled(true);
        }
    }
    
    /**
     * Handles clicks in the game selection GUI.
     */
    private void handleGameSelectionClick(Player player, ItemStack clickedItem) {
        String itemName = ChatColor.stripColor(clickedItem.getItemMeta().getDisplayName());
        
        if (itemName.equals("Create New Game")) {
            player.closeInventory();
            guiManager.removeOpenInventory(player);
            guiManager.showWorldSelectionGui(player);
        } else if (clickedItem.getType() == Material.COMPASS) {
            // Join existing game
            player.closeInventory();
            guiManager.removeOpenInventory(player);
            
            String gameName = ChatColor.stripColor(itemName);
            Game game = gameManager.getGame(gameName);
            
            if (game != null) {
                // Add player to game
                boolean joined = gameManager.addPlayerToGame(player, game);
                if (joined) {
                    player.sendMessage(ChatColor.GREEN + "Joined game: " + ChatColor.GOLD + gameName);
                    // Lobby items are given in addPlayerToGame when in lobby state
                } else {
                    player.sendMessage(ChatColor.RED + "Failed to join game: " + gameName);
                }
            } else {
                player.sendMessage(ChatColor.RED + "Game no longer exists!");
            }
        }
    }
    
    /**
     * Handles clicks in the world selection GUI.
     */
    private void handleWorldSelectionClick(Player player, ItemStack clickedItem) {
        String itemName = ChatColor.stripColor(clickedItem.getItemMeta().getDisplayName());
        
        Material block = null;
        if (this.plugin.getServer().getBukkitVersion().contains("1.8.")) {
            block = Material.GRASS; // legacy
        } else {
            block = Material.GRASS_BLOCK;
        }
        if (clickedItem.getType() == block && itemName.equals("Generate New World")) {
            player.closeInventory();
            guiManager.removeOpenInventory(player);
            
            // Show a message that world is being generated
            player.sendMessage(ChatColor.GREEN + "Generating new world for your manhunt game...");
            
            // Generate a game name
            String gameName = "manhunt_Game_" + player.getName() + "_" + random.nextInt(1000);
            
            // Generate a new world using the WorldManagementService
            plugin.getWorldManagementService().createWorldWithMultiverse(gameName).thenAccept(world -> {
                if (world != null) {
                    // Create the game with the new world
                    plugin.getServer().getScheduler().runTask(plugin, () -> {
                        boolean created = gameManager.createGame(gameName, player, world);
                        
                        if (created) {
                            player.sendMessage(ChatColor.GREEN + "Created new game in a fresh world with name: " + 
                                    ChatColor.GOLD + gameName);
                            
                            // Join the game
                            Game game = gameManager.getGame(gameName);
                            if (game != null) {
                                // First teleport the game creator to the lobby capsule
                                boolean teleportSuccess = gameManager.teleportToLobbyCapsule(player, game);
                                
                                if (teleportSuccess) {
                                    // Then give player lobby items after teleporting
                                    guiManager.giveLobbyItems(player, game);
                                } else {
                                    // If teleport failed, delete the new game
                                    gameManager.deleteGame(gameName);
                                    player.sendMessage(ChatColor.RED + "Failed to teleport to the lobby capsule. Game creation aborted.");
                                }
                            }
                        } else {
                            player.sendMessage(ChatColor.RED + "Failed to create game.");
                        }
                    });
                } else {
                    player.sendMessage(ChatColor.RED + "Failed to generate world. Please try again.");
                }
            }).exceptionally(ex -> {
                player.sendMessage(ChatColor.RED + "An error occurred while generating the world: " + ex.getMessage());
                ex.printStackTrace();
                return null;
            });
        } else if (itemName.equals("Back")) {
            player.closeInventory();
            guiManager.removeOpenInventory(player);
            guiManager.showGameSelectionGui(player);
        }
        // Clicking on a world that's in use (Material.RED_STAINED_GLASS) will do nothing
        else if (clickedItem.getType() == Material.RED_STAINED_GLASS) {
            // Extract the original world name from the "WorldName (In Use)" format
            String worldDisplayName = itemName.replace(" (In Use)", "");
            player.sendMessage(ChatColor.RED + "The world " + worldDisplayName + " is currently in use by another game.");
        }
        // Clicking on non-loaded worlds (Material.BARRIER)
        else if (clickedItem.getType() == Material.BARRIER) {
            player.sendMessage(ChatColor.RED + "This world is not loaded. Ask an admin to load it first.");
        }
    }
    
    /**
     * Handles clicks in the team selection GUI.
     */
    private void handleTeamSelectionClick(Player player, ItemStack clickedItem) {
        String itemName = ChatColor.stripColor(clickedItem.getItemMeta().getDisplayName());
        Game game = gameManager.getPlayerGame(player);
        
        if (game == null) {
            player.closeInventory();
            guiManager.removeOpenInventory(player);
            player.sendMessage(ChatColor.RED + "You are no longer in a game.");
            return;
        }
        
        if (itemName.equals("Join Runners")) {
            boolean wasHunter = game.isHunter(player);
            
            game.addRunner(player);
            player.sendMessage(ChatColor.GREEN + "You joined the " + ChatColor.BLUE + "RUNNERS" + ChatColor.GREEN + " team.");
            player.closeInventory();
            guiManager.removeOpenInventory(player);
            
            // Update the boss bar to reflect team changes
            gameManager.updateLobbyBossBar(game);
            
            // Broadcast team change if player was on a different team
            if (wasHunter) {
                for (UUID playerId : game.getAllPlayers()) {
                    Player p = Bukkit.getPlayer(playerId);
                    if (p != null && !p.equals(player)) {
                        p.sendMessage(ChatColor.YELLOW + player.getName() + " switched from " + 
                            ChatColor.RED + "HUNTERS" + ChatColor.YELLOW + " to " + 
                            ChatColor.BLUE + "RUNNERS" + ChatColor.YELLOW + " team.");
                    }
                }
            }
            
            // Give lobby items
            guiManager.giveLobbyItems(player, game);
        } else if (itemName.equals("Join Hunters")) {
            boolean wasRunner = game.isRunner(player);
            
            game.addHunter(player);
            player.sendMessage(ChatColor.GREEN + "You joined the " + ChatColor.RED + "HUNTERS" + ChatColor.GREEN + " team.");
            player.closeInventory();
            guiManager.removeOpenInventory(player);
            
            // Update the boss bar to reflect team changes
            gameManager.updateLobbyBossBar(game);
            
            // Broadcast team change if player was on a different team
            if (wasRunner) {
                for (UUID playerId : game.getAllPlayers()) {
                    Player p = Bukkit.getPlayer(playerId);
                    if (p != null && !p.equals(player)) {
                        p.sendMessage(ChatColor.YELLOW + player.getName() + " switched from " + 
                            ChatColor.BLUE + "RUNNERS" + ChatColor.YELLOW + " to " + 
                            ChatColor.RED + "HUNTERS" + ChatColor.YELLOW + " team.");
                    }
                }
            }
            
            // Give lobby items
            guiManager.giveLobbyItems(player, game);
        }
    }
    
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        
        if (item == null || !item.hasItemMeta() || !item.getItemMeta().hasDisplayName()) {
            return;
        }
        
        Game game = gameManager.getPlayerGame(player);
        if (game == null) {
            return;
        }
        
        // Ensure we check for ANY interaction with lobby items, not just right clicks
        // This way the items will always execute their command
        if (game.getState().toString().startsWith("LOBBY")) {
            String itemName = ChatColor.stripColor(item.getItemMeta().getDisplayName());
            
            if (itemName.equals("Switch Team") || 
                itemName.equals("Leave Game") || 
                itemName.equals("Force Start") || 
                itemName.equals("Delete Game")) {
                
                event.setCancelled(true);
                
                // Process the action regardless of click type
                if (itemName.equals("Switch Team")) {
                    guiManager.showTeamSelectionGui(player, game);
                } else if (itemName.equals("Leave Game")) {
                    gameManager.removePlayerFromGame(player);
                    player.sendMessage(ChatColor.GREEN + "You left the game.");
                } else if (itemName.equals("Force Start") && game.isOwner(player)) {
                    player.sendMessage(ChatColor.YELLOW + "Starting game...");
                    
                    // Handle the CompletableFuture return type
                    gameManager.startGame(game).thenAccept(started -> {
                        if (!started) {
                            player.sendMessage(ChatColor.RED + "Failed to start game. Make sure there is at least one hunter and one runner.");
                        }
                    }).exceptionally(ex -> {
                        player.sendMessage(ChatColor.RED + "An error occurred while starting the game: " + ex.getMessage());
                        plugin.getLogger().severe("Error starting game: " + ex.getMessage());
                        return null;
                    });
                } else if (itemName.equals("Delete Game") && game.isOwner(player)) {
                    gameManager.deleteGame(game.getName());
                    player.sendMessage(ChatColor.GREEN + "Game deleted.");
                }
            }
        }
    }
    
    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItemInHand();
        
        // Cancel placement of any lobby items
        if (item != null && item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
            String itemName = ChatColor.stripColor(item.getItemMeta().getDisplayName());
            
            if (itemName.equals("Switch Team") || 
                itemName.equals("Leave Game") || 
                itemName.equals("Force Start") || 
                itemName.equals("Delete Game")) {
                
                event.setCancelled(true);
                
                // Trigger the interaction event to ensure the command executes
                PlayerInteractEvent interactEvent = new PlayerInteractEvent(
                    player, 
                    Action.RIGHT_CLICK_AIR, 
                    item, 
                    null, 
                    null
                );
                plugin.getServer().getPluginManager().callEvent(interactEvent);
            }
        }
    }

    @EventHandler
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        Game game = gameManager.getPlayerGame(player);
        
        // Prevent dropping lobby items
        if (game != null && game.getState().toString().startsWith("LOBBY")) {
            ItemStack item = event.getItemDrop().getItemStack();
            
            if (item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
                String name = ChatColor.stripColor(item.getItemMeta().getDisplayName());
                
                // Check if it's a lobby item
                if (name.equals("Switch Team") || name.equals("Leave Game") || 
                        name.equals("Force Start") || name.equals("Delete Game")) {
                    event.setCancelled(true);
                }
            }
        }
    }
} 