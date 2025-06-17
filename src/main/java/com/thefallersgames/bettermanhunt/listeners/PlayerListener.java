package com.thefallersgames.bettermanhunt.listeners;

import com.thefallersgames.bettermanhunt.Plugin;
import com.thefallersgames.bettermanhunt.managers.GameManager;
import com.thefallersgames.bettermanhunt.managers.TeamChatManager;
import com.thefallersgames.bettermanhunt.models.Game;
import com.thefallersgames.bettermanhunt.models.GameState;
import com.thefallersgames.bettermanhunt.tasks.CompassTask;
import com.thefallersgames.bettermanhunt.utils.GameUtils;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.EnderDragon;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Listener for player-related events in the Manhunt game.
 */
public class PlayerListener implements Listener {
    private final Plugin plugin;
    private final GameManager gameManager;
    private final TeamChatManager teamChatManager;
    private final Map<UUID, Long> compassCooldowns;
    
    /**
     * Creates a new player listener.
     *
     * @param plugin The plugin instance
     * @param gameManager The game manager
     * @param teamChatManager The team chat manager to use
     */
    public PlayerListener(Plugin plugin, GameManager gameManager, TeamChatManager teamChatManager) {
        this.plugin = plugin;
        this.gameManager = gameManager;
        this.teamChatManager = teamChatManager;
        this.compassCooldowns = new HashMap<>();
    }
    
    /**
     * Handles player death events.
     */
    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        Game game = gameManager.getPlayerGame(player);
        
        if (game != null && (game.getState() == GameState.ACTIVE || game.getState() == GameState.GAME_ENDED)) {
            if (game.isRunner(player)) {
                // Handle runner death
                gameManager.handleRunnerDeath(player);
                
                // Update the active game boss bar to reflect fewer runners
                gameManager.updateActiveGameBossBar(game);
                
                // Broadcast death message
                String deathMessage = ChatColor.RED + player.getName() + " has died! They are now spectating.";
                GameUtils.broadcastMessageToGame(plugin, game, deathMessage);
            }
            
            // Schedule immediate respawn for all players in the game
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                player.spigot().respawn();
            }, 1L);
        }
    }
    
    /**
     * Handles player respawn events to ensure correct gamemode.
     */
    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        Game game = gameManager.getPlayerGame(player);
        
        if (game != null && game.getState() == GameState.ACTIVE) {
            if (game.isHunter(player)) {
                // Hunters always respawn in SURVIVAL mode (never back to headstart frozen state)
                player.setGameMode(GameMode.SURVIVAL);
                
                // Give them a new compass if they don't have one
                scheduleCompassCheck(player, game);
            } else if (game.isSpectator(player)) {
                // Spectators (including dead runners) stay in spectator mode
                player.setGameMode(GameMode.SPECTATOR);
            }
        }
    }
    
    /**
     * Helper method to check and give a new compass after a delay
     */
    private void scheduleCompassCheck(Player player, Game game) {
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (GameUtils.hasCompass(player)) return;
            
            CompassTask compassTask = gameManager.getCompassTask(game.getName());
            if (compassTask != null) {
                compassTask.giveTrackingCompass(player);
            }
        }, 5L); // Slight delay to ensure inventory is set up
    }
    
    /**
     * Handles player quit events.
     */
    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        Game game = gameManager.getPlayerGame(player);
        
        if (game != null) {
            // If the player is a runner in an active game, treat it as a death
            if (game.getState() == GameState.ACTIVE && game.isRunner(player)) {
                gameManager.handleRunnerDeath(player);
                
                // Broadcast quit message
                String quitMessage = ChatColor.RED + player.getName() + " has left the game! They are out of the manhunt.";
                GameUtils.broadcastMessageToGame(plugin, game, quitMessage);
            }
            
            // Remove player from the game
            gameManager.removePlayerFromGame(player);
        }
        
        // Clean up team chat preference and compass cooldown
        teamChatManager.removePlayer(player);
        compassCooldowns.remove(player.getUniqueId());
    }
    
    /**
     * Handles player right-click events for compass tracking.
     */
    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        
        // Only process right-clicks with an item
        if (event.getAction() != Action.RIGHT_CLICK_AIR && 
            event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        
        ItemStack item = event.getItem();
        
        // Check if this is a tracking compass
        if (item == null || item.getType() != Material.COMPASS) {
            return;
        }
        
        Game game = gameManager.getPlayerGame(player);
        
        // Verify this player is a hunter in an active game
        if (game == null || game.getState() != GameState.ACTIVE || !game.isHunter(player)) {
            return;
        }
        
        // Get the compass task
        CompassTask compassTask = gameManager.getCompassTask(game.getName());
        if (compassTask == null || !compassTask.isTrackingCompass(item)) {
            return;
        }
        
        // Apply cooldown to prevent spam
        if (isCompassOnCooldown(player)) {
            player.sendMessage(ChatColor.RED + "Tracking compass is recharging... Please wait!");
            event.setCancelled(true);
            return;
        }
        
        // Update the compass
        if (compassTask.updateCompassTarget(player)) {
            player.playSound(player.getLocation(), Sound.BLOCK_BEACON_POWER_SELECT, 0.5f, 2.0f);
            setCompassCooldown(player);
        }
        
        // Cancel the event to prevent normal compass behavior
        event.setCancelled(true);
    }
    
    /**
     * Checks if a player's compass is on cooldown
     */
    private boolean isCompassOnCooldown(Player player) {
        long currentTime = System.currentTimeMillis();
        long lastUse = compassCooldowns.getOrDefault(player.getUniqueId(), 0L);
        return (currentTime - lastUse < 2000); // 2 second cooldown
    }
    
    /**
     * Sets the compass cooldown for a player
     */
    private void setCompassCooldown(Player player) {
        compassCooldowns.put(player.getUniqueId(), System.currentTimeMillis());
    }
    
    /**
     * Prevents inventory interaction in lobby.
     */
    @EventHandler(priority = EventPriority.NORMAL)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }
        
        Player player = (Player) event.getWhoClicked();
        Game game = gameManager.getPlayerGame(player);
        
        // Prevent moving items in lobby state only
        if (game != null && game.getState() == GameState.LOBBY) {
            event.setCancelled(true);
        }
    }
    
    /**
     * Handles player chat events for team chat.
     */
    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        teamChatManager.handleChat(event);
    }
    
    /**
     * Handles Ender Dragon death to detect runner victory.
     */
    @EventHandler(priority = EventPriority.NORMAL)
    public void onEntityDeath(EntityDeathEvent event) {
        if (!(event.getEntity() instanceof EnderDragon)) {
            return;
        }
        
        // Check if the killer is a runner
        if (event.getEntity().getKiller() instanceof Player) {
            Player player = event.getEntity().getKiller();
            Game game = gameManager.getPlayerGame(player);
            
            if (game != null && game.getState() == GameState.ACTIVE && game.isRunner(player)) {
                // Runner won by killing the dragon!
                gameManager.endGame(game, true);
                
                // Broadcast victory message
                String victoryMessage = ChatColor.GREEN + "The dragon has been defeated! " + 
                        ChatColor.GOLD + player.getName() + ChatColor.GREEN + " has won the manhunt!";
                
                GameUtils.broadcastMessageToGame(plugin, game, victoryMessage);
            }
        }
    }
    
    /**
     * Toggles team chat for a player.
     *
     * @param player The player to toggle team chat for
     * @return True if team chat is now enabled, false if disabled
     */
    public boolean toggleTeamChat(Player player) {
        return teamChatManager.toggleTeamChat(player);
    }
    
    /**
     * Checks if team chat is enabled for a player.
     *
     * @param player The player to check
     * @return True if team chat is enabled, false otherwise
     */
    public boolean isTeamChatEnabled(Player player) {
        return teamChatManager.isTeamChatEnabled(player);
    }
} 