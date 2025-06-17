package com.thefallersgames.bettermanhunt.listeners;

import com.thefallersgames.bettermanhunt.models.Game;
import com.thefallersgames.bettermanhunt.models.GameState;
import com.thefallersgames.bettermanhunt.managers.GameManager;
import com.thefallersgames.bettermanhunt.tasks.CompassTask;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Iterator;

/**
 * Listener for protecting special game items like compasses during active gameplay.
 */
public class GameItemProtectionListener implements Listener {
    private final GameManager gameManager;

    /**
     * Constructs a new GameItemProtectionListener.
     *
     * @param gameManager The game manager
     */
    public GameItemProtectionListener(GameManager gameManager) {
        this.gameManager = gameManager;
    }

    /**
     * Prevents compass items from dropping when players die.
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        Game game = gameManager.getPlayerGame(player);
        
        if (game == null || game.getState() != GameState.ACTIVE) {
            return;
        }
        
        // Remove compass items from drops when a player dies during active gameplay
        Iterator<ItemStack> iterator = event.getDrops().iterator();
        while (iterator.hasNext()) {
            ItemStack item = iterator.next();
            if (item.getType().name().contains("COMPASS")) {
                iterator.remove();
            }
        }
    }
    
    /**
     * Prevents players from dropping compass items.
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        Game game = gameManager.getPlayerGame(player);
        
        // Only care about active games
        if (game == null || game.getState() != GameState.ACTIVE) {
            return;
        }
        
        // Prevent dropping compass items
        ItemStack item = event.getItemDrop().getItemStack();
        if (item.getType().name().contains("COMPASS")) {
            event.setCancelled(true);
            player.sendMessage(ChatColor.RED + "You cannot drop your tracking compass!");
        }
    }
    
    /**
     * Prevents moving tracking compasses between inventory slots via click.
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }
        
        Player player = (Player) event.getWhoClicked();
        Game game = gameManager.getPlayerGame(player);
        
        if (game == null || game.getState() != GameState.ACTIVE) {
            return;
        }
        
        // Get the compass task
        CompassTask compassTask = gameManager.getCompassTask(game.getName());
        if (compassTask == null) {
            return;
        }
        
        // Protect tracking compass from specific inventory actions
        ItemStack item = event.getCurrentItem();
        if (item != null && item.getType().name().contains("COMPASS")) {
            if (compassTask.isTrackingCompass(item) && 
                (event.getClick().isShiftClick() || event.isRightClick())) {
                event.setCancelled(true);
                player.sendMessage(ChatColor.RED + "You cannot move the tracking compass!");
            }
        }
    }
} 