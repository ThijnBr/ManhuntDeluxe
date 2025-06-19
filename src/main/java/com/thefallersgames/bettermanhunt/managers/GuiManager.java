package com.thefallersgames.bettermanhunt.managers;

import com.thefallersgames.bettermanhunt.Plugin;
import com.thefallersgames.bettermanhunt.models.Game;
import com.thefallersgames.bettermanhunt.utils.GuiUtil;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Manages all GUI related functionalities for the Manhunt plugin.
 */
public class GuiManager {
    private final Plugin plugin;
    private final GameManager gameManager;
    
    // Store inventory types for GUI click handling
    private final Map<UUID, String> openInventories = new HashMap<>();
    
    // Inventory names/identifiers
    public static final String GAME_SELECTION_GUI = "game_selection";
    public static final String WORLD_SELECTION_GUI = "world_selection";
    public static final String TEAM_SELECTION_GUI = "team_selection";
    
    /**
     * Creates a new GuiManager.
     *
     * @param plugin The plugin instance
     * @param gameManager The game manager
     */
    public GuiManager(Plugin plugin, GameManager gameManager) {
        this.plugin = plugin;
        this.gameManager = gameManager;
    }
    
    /**
     * Shows the game selection GUI to a player.
     * This GUI allows players to join existing games or create a new one.
     *
     * @param player The player to show the GUI to
     */
    public void showGameSelectionGui(Player player) {
        Inventory gui = GuiUtil.createBorderedGui("&8&lManhunt - Game Selection", 45, Material.GLASS);
        
        // Create New Game button
        gui.setItem(22, GuiUtil.createItem(Material.NETHER_STAR, "&a&lCreate New Game",
                "&7Click to create a new Manhunt game"));
        
        Collection<Game> games = gameManager.getAllGames();
        
        if (games.isEmpty()) {
            gui.setItem(31, GuiUtil.createItem(Material.BARRIER, "&c&lNo Active Games",
                    "&7There are no active games.",
                    "&7Click 'Create New Game' to make one!"));
        } else {
            int slot = 10;
            for (Game game : games) {
                if (slot > 34) break; // Prevent overflow
                
                String stateName = game.getState().name();
                String stateColor = game.getState().toString().startsWith("LOBBY") ? "&a" : "&c";
                
                gui.setItem(slot, GuiUtil.createItem(Material.COMPASS, "&b" + game.getName(),
                        "&7Status: " + stateColor + stateName,
                        "&7Runners: &e" + game.getRunners().size(),
                        "&7Hunters: &e" + game.getHunters().size(),
                        "",
                        "&eClick to join"));
                
                // Increase slot, skip border columns
                slot++;
                if (slot % 9 == 8) slot += 2;
            }
        }
        
        GuiUtil.fillEmptySlots(gui, Material.LIGHT_GRAY_STAINED_GLASS_PANE);
        player.openInventory(gui);
        openInventories.put(player.getUniqueId(), GAME_SELECTION_GUI);
    }
    
    /**
     * Shows the world selection GUI to a player.
     * This GUI allows players to select whether to use the current world or generate a new one.
     *
     * @param player The player to show the GUI to
     */
    
    public void showWorldSelectionGui(Player player) {
        Inventory gui = GuiUtil.createBorderedGui("&8&lManhunt - World Selection", 45, Material.GLASS);
        player.sendMessage(ChatColor.GREEN + "Server version: " + this.plugin.getServer().getBukkitVersion());
        Material block = null;
        if (this.plugin.getServer().getBukkitVersion().contains("1.8.")) {
            block = Material.GRASS; // legacy
        } else {
            block = Material.GRASS_BLOCK;
        }
        // Create a "Generate New World" option
        gui.setItem(22, GuiUtil.createItem(block, "&a&lGenerate New World",
                "&7Click to create a new world for the game.",
                "&7A fresh world will be generated with",
                "&7Nether and End dimensions."));
        
        // Back button
        gui.setItem(40, GuiUtil.createItem(Material.ARROW, "&c&lBack",
                "&7Return to game selection"));

        if (!this.plugin.getServer().getBukkitVersion().contains("1.8.")) {
            GuiUtil.fillEmptySlots(gui, Material.LIGHT_GRAY_STAINED_GLASS_PANE);
        } else {
            GuiUtil.fillEmptySlots(gui, Material.GLASS);
        }
                
        player.openInventory(gui);
        openInventories.put(player.getUniqueId(), WORLD_SELECTION_GUI);
    }
    
    /**
     * Shows the team selection GUI to a player.
     * This GUI allows players to select their team (runner or hunter).
     *
     * @param player The player to show the GUI to
     * @param game The game instance
     */
    public void showTeamSelectionGui(Player player, Game game) {
        Inventory gui = GuiUtil.createBorderedGui("&8&lManhunt - Team Selection", 27, Material.GLASS);
        
        // Runner team option
        gui.setItem(11, GuiUtil.createItem(Material.DIAMOND_BOOTS, "&b&lJoin Runners",
                "&7Join the runners team",
                "&7Try to beat the game while being hunted!"));
        
        // Hunter team option
        gui.setItem(15, GuiUtil.createItem(Material.COMPASS, "&c&lJoin Hunters",
                "&7Join the hunters team",
                "&7Track down and eliminate the runners!"));
        
        // Display current team
        String currentTeam = "None";
        if (game.isRunner(player)) {
            currentTeam = "&bRunners";
        } else if (game.isHunter(player)) {
            currentTeam = "&cHunters";
        }
        
        gui.setItem(22, GuiUtil.createItem(Material.NAME_TAG, "&e&lCurrent Team: " + currentTeam));
        
        player.openInventory(gui);
        openInventories.put(player.getUniqueId(), TEAM_SELECTION_GUI);
    }
    
    /**
     * Records the type of GUI a player has open.
     *
     * @param player The player
     * @param inventoryType The type of inventory they have open
     */
    public void setOpenInventory(Player player, String inventoryType) {
        openInventories.put(player.getUniqueId(), inventoryType);
    }
    
    /**
     * Gets the type of GUI a player has open.
     *
     * @param player The player
     * @return The type of GUI, or null if none
     */
    public String getOpenInventoryType(Player player) {
        return openInventories.get(player.getUniqueId());
    }
    
    /**
     * Removes a player from the open inventories map.
     *
     * @param player The player to remove
     */
    public void removeOpenInventory(Player player) {
        openInventories.remove(player.getUniqueId());
    }
    
    /**
     * Gives a player the appropriate lobby items based on their role in the game.
     *
     * @param player The player
     * @param game The game instance
     */
    public void giveLobbyItems(Player player, Game game) {
        player.getInventory().clear();
        
        // Team switcher (slot 0) - for all players
        player.getInventory().setItem(0, GuiUtil.createItem(Material.NETHER_STAR, "&e&lSwitch Team",
                "&7Click to switch between teams"));
        
        // Only give these items to the owner
        if (game.isOwner(player)) {
            // Force start (slot 4)
            player.getInventory().setItem(4, GuiUtil.createItem(Material.EMERALD, "&a&lForce Start",
                    "&7Click to start the game"));
                    
            // Delete game (slot 7) - no leave item for owner as delete covers this functionality
            player.getInventory().setItem(7, GuiUtil.createItem(Material.TNT, "&c&lDelete Game",
                    "&7Click to delete the game"));
        } else {
            // Leave game (slot 8) - only for non-owners
            player.getInventory().setItem(8, GuiUtil.createItem(Material.BARRIER, "&c&lLeave Game",
                    "&7Click to leave the game"));
        }
        
        player.updateInventory();
    }
} 