package com.thefallersgames.bettermanhunt.tasks;

import com.thefallersgames.bettermanhunt.Plugin;
import com.thefallersgames.bettermanhunt.models.Game;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.Arrays;
import java.util.UUID;

/**
 * Class that manages tracking compasses for hunters to point to the nearest runner.
 * Compasses only update on right-click and cannot be dropped.
 */
public class CompassTask {
    private final Game game;
    private final NamespacedKey trackerKey;
    
    /**
     * Creates a new compass tracking manager.
     *
     * @param plugin The plugin instance
     * @param game The game this task is for
     */
    public CompassTask(Plugin plugin, Game game) {
        this.game = game;
        this.trackerKey = new NamespacedKey(plugin, "runner_tracker");
        
        // Give all hunters a tracking compass
        for (UUID hunterId : game.getHunters()) {
            Player hunter = Bukkit.getPlayer(hunterId);
            if (hunter != null) {
                giveTrackingCompass(hunter);
            }
        }
    }
    
    /**
     * Updates a compass to point to the nearest runner for a specific hunter.
     * Called when a hunter right-clicks their compass.
     *
     * @param hunter The hunter who clicked their compass
     * @return True if a target was found and compass was updated, false otherwise
     */
    public boolean updateCompassTarget(Player hunter) {
        // Find nearest runner
        Player nearestRunner = findNearestRunner(hunter);
        if (nearestRunner == null) {
            hunter.sendMessage("§cNo runners found to track!");
            return false;
        }
        
        // Update the hunter's compass
        updateCompass(hunter, nearestRunner);
        
        return true;
    }
    
    /**
     * Finds the nearest runner to a hunter.
     *
     * @param hunter The hunter looking for a runner
     * @return The nearest runner, or null if no runners are online
     */
    private Player findNearestRunner(Player hunter) {
        Player nearestRunner = null;
        double nearestDistance = Double.MAX_VALUE;
        
        boolean sameWorldRunnerExists = false;
        boolean diffWorldRunnerExists = false;
        
        for (UUID runnerId : game.getRunners()) {
            Player runner = Bukkit.getPlayer(runnerId);
            if (runner == null || !runner.isOnline()) {
                continue;
            }
            
            // Track if runners exist in different worlds
            if (runner.getWorld() != hunter.getWorld()) {
                diffWorldRunnerExists = true;
                continue;
            }
            
            sameWorldRunnerExists = true;
            double distance = hunter.getLocation().distance(runner.getLocation());
            if (distance < nearestDistance) {
                nearestDistance = distance;
                nearestRunner = runner;
            }
        }
        
        // If no runner found in same world but runners exist in other worlds
        if (nearestRunner == null && !sameWorldRunnerExists && diffWorldRunnerExists) {
            hunter.sendMessage("§cCannot track runners in different dimensions!");
        }
        
        return nearestRunner;
    }
    
    /**
     * Updates a hunter's compass to point to a runner.
     *
     * @param hunter The hunter whose compass to update
     * @param target The target runner
     */
    private void updateCompass(Player hunter, Player target) {
        ItemStack compass = null;
        int slot = -1;
        
        // Find compass in inventory
        for (int i = 0; i < hunter.getInventory().getSize(); i++) {
            ItemStack item = hunter.getInventory().getItem(i);
            if (item != null && item.getType() == Material.COMPASS && 
                    item.getItemMeta() != null && 
                    item.getItemMeta().getPersistentDataContainer().has(trackerKey, PersistentDataType.BYTE)) {
                compass = item;
                slot = i;
                break;
            }
        }
        
        // If hunter doesn't have a compass, give them one
        if (compass == null) {
            giveTrackingCompass(hunter);
            return;
        }
        
        // Update compass target
        Location targetLoc = target.getLocation();
        hunter.setCompassTarget(targetLoc);
        
        // Calculate distance between hunter and target
        double distance = hunter.getLocation().distance(targetLoc);
        
        // Update the lore with coordinates and distance
        ItemMeta meta = compass.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§cRunner Tracker");
            meta.setLore(Arrays.asList(
                "§fTracking: §e" + target.getName(),
                "§fDistance: §e" + ((int) distance) + " blocks",
                "§fLocation: §e" + targetLoc.getBlockX() + ", " + targetLoc.getBlockY() + ", " + targetLoc.getBlockZ(),
                "§fWorld: §e" + target.getWorld().getName(),
                "",
                "§6Right-click to update target"
            ));
            compass.setItemMeta(meta);
            hunter.getInventory().setItem(slot, compass);
        }
        
        // Send message about the target
        hunter.sendMessage("§fCompass is now tracking §e" + target.getName() + 
                " §f(§e" + ((int) distance) + " blocks away§f)");
    }
    
    /**
     * Gives a hunter a tracking compass.
     *
     * @param hunter The hunter to give the compass to
     */
    public void giveTrackingCompass(Player hunter) {
        ItemStack compass = new ItemStack(Material.COMPASS);
        ItemMeta meta = compass.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§cRunner Tracker");
            meta.setLore(Arrays.asList(
                "§fRight-click to find the nearest runner.",
                "§fThe compass will update to point to them.",
                "",
                "§cThis compass cannot be dropped or transferred."
            ));
            
            // Make it undroppable by adding an NBT tag
            meta.getPersistentDataContainer().set(trackerKey, PersistentDataType.BYTE, (byte) 1);
            
            // Add item flags to make it look special
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            meta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE);
            meta.setUnbreakable(true);
            
            compass.setItemMeta(meta);
        }
        
        hunter.getInventory().addItem(compass);
        hunter.sendMessage("§6You have been given a §cRunner Tracking Compass§6!");
        hunter.sendMessage("§6Right-click it to track the nearest runner.");
    }
    
    /**
     * Checks if the given item is a runner tracking compass.
     * 
     * @param item The item to check
     * @return True if it's a tracking compass, false otherwise
     */
    public boolean isTrackingCompass(ItemStack item) {
        if (item == null || item.getType() != Material.COMPASS) {
            return false;
        }
        
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return false;
        }
        
        return meta.getPersistentDataContainer().has(trackerKey, PersistentDataType.BYTE);
    }
    
    /**
     * Gets the tracker key used to identify tracking compasses.
     * 
     * @return The NamespacedKey for tracker compasses
     */
    public NamespacedKey getTrackerKey() {
        return trackerKey;
    }
} 