package com.thefallersgames.bettermanhunt.listeners;

import com.thefallersgames.bettermanhunt.models.Game;
import com.thefallersgames.bettermanhunt.models.GameState;
import com.thefallersgames.bettermanhunt.managers.GameManager;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Iterator;
import java.util.List;

/**
 * Listener for protecting players and items in the lobby.
 */
public class LobbyProtectionListener implements Listener {
    private final GameManager gameManager;

    /**
     * Constructs a new LobbyProtectionListener.
     *
     * @param gameManager The game manager
     */
    public LobbyProtectionListener(GameManager gameManager) {
        this.gameManager = gameManager;
    }

    /**
     * Prevents lobby items from dropping on death.
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        Game game = gameManager.getPlayerGame(player);
        
        if (game == null || game.getState() != GameState.LOBBY) {
            return;
        }
        
        // Remove lobby items from drops when in lobby
        Iterator<ItemStack> iterator = event.getDrops().iterator();
        while (iterator.hasNext()) {
            ItemStack item = iterator.next();
            
            // Remove lobby items
            if (isLobbyItem(item)) {
                iterator.remove();
            }
        }
    }
    
    /**
     * Makes players invulnerable in lobby.
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            return;
        }
        
        Player player = (Player) event.getEntity();
        Game game = gameManager.getPlayerGame(player);
        
        // Check if player is in a lobby
        if (game != null && game.getState() == GameState.LOBBY) {
            event.setCancelled(true);
        }
    }
    
    /**
     * Prevents player vs player damage in lobby.
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        // Cancel player damage by other entities if either is in a lobby
        if (event.getDamager() instanceof Player) {
            Player damager = (Player) event.getDamager();
            Game damagerGame = gameManager.getPlayerGame(damager);
            
            if (damagerGame != null && damagerGame.getState() == GameState.LOBBY) {
                event.setCancelled(true);
            }
        }
        
        if (event.getEntity() instanceof Player) {
            Player damaged = (Player) event.getEntity();
            Game damagedGame = gameManager.getPlayerGame(damaged);
            
            if (damagedGame != null && damagedGame.getState() == GameState.LOBBY) {
                event.setCancelled(true);
            }
        }
    }

    /**
     * Prevents hunger level changes in lobby.
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onFoodLevelChange(FoodLevelChangeEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            return;
        }
        
        Player player = (Player) event.getEntity();
        Game game = gameManager.getPlayerGame(player);
        
        // Keep hunger full in lobby
        if (game != null && game.getState() == GameState.LOBBY) {
            event.setCancelled(true);
            event.setFoodLevel(20); // Max food level
            player.setSaturation(20f); // Max saturation
        }
    }
    
    /**
     * Prevents block breaking in lobby.
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Game game = gameManager.getPlayerGame(player);
        
        // Prevent block breaking in lobby
        if (game != null && game.getState() == GameState.LOBBY) {
            event.setCancelled(true);
            player.sendMessage(ChatColor.RED + "You cannot break blocks in the lobby!");
        }
    }
    
    /**
     * Checks if an item is a lobby item.
     *
     * @param item The item to check
     * @return True if the item is a lobby item, false otherwise
     */
    private boolean isLobbyItem(ItemStack item) {
        if (item == null || !item.hasItemMeta() || !item.getItemMeta().hasDisplayName()) {
            return false;
        }
        
        String name = ChatColor.stripColor(item.getItemMeta().getDisplayName());
        return name.equals("Switch Team") || 
               name.equals("Leave Game") || 
               name.equals("Force Start") || 
               name.equals("Delete Game");
    }
} 