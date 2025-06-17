package com.thefallersgames.bettermanhunt.tasks;

import com.thefallersgames.bettermanhunt.Plugin;
import com.thefallersgames.bettermanhunt.models.Game;
import com.thefallersgames.bettermanhunt.managers.GameManager;
import com.thefallersgames.bettermanhunt.services.GameTaskService;
import org.bukkit.Bukkit;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.UUID;

/**
 * Task that handles the headstart countdown for hunters.
 */
public class HeadstartTask extends BukkitRunnable {
    private final Game game;
    private final BossBar bossBar;
    private int timeLeft;
    private final GameManager gameManager;
    private final GameTaskService gameTaskService;

    /**
     * Creates a new headstart task.
     *
     * @param plugin The plugin instance
     * @param game The game this task is for
     * @param bossBar The boss bar to update with countdown progress
     * @param gameManager The game manager to handle unfreezing hunters
     * @param gameTaskService The game task service for creating active game boss bar
     */
    public HeadstartTask(Plugin plugin, Game game, BossBar bossBar, GameManager gameManager, GameTaskService gameTaskService) {
        this.game = game;
        this.bossBar = bossBar;
        this.timeLeft = game.getHeadstartDuration();
        this.gameManager = gameManager;
        this.gameTaskService = gameTaskService;
    }

    @Override
    public void run() {
        if (timeLeft <= 0) {
            // Headstart time is over, release the hunters!
            
            // Unfreeze hunters
            gameManager.unfreezeHunters(game);
            
            // Notify hunters
            for (UUID hunterId : game.getHunters()) {
                Player hunter = Bukkit.getPlayer(hunterId);
                if (hunter != null) {
                    hunter.sendMessage("§6The headstart is over! Hunt the runners!");
                }
            }
            
            // Notify runners
            for (UUID runnerId : game.getRunners()) {
                Player runner = Bukkit.getPlayer(runnerId);
                if (runner != null) {
                    runner.sendMessage("§cThe hunters have been released! Run!");
                }
            }
            
            // Update the active game boss bar immediately
            gameTaskService.updateActiveGameBossBar(game);
            
            this.cancel(); // End the task
            return;
        }
        
        // Update boss bar
        bossBar.setProgress(timeLeft / (double) game.getHeadstartDuration());
        bossBar.setTitle("Headstart: " + timeLeft + " seconds remaining");
        
        // Countdown message
        if (timeLeft <= 5 || timeLeft % 10 == 0) {
            for (UUID hunterId : game.getHunters()) {
                Player hunter = Bukkit.getPlayer(hunterId);
                if (hunter != null) {
                    hunter.sendMessage("§6You will be released in §e" + timeLeft + "§6 seconds!");
                }
            }
            
            for (UUID runnerId : game.getRunners()) {
                Player runner = Bukkit.getPlayer(runnerId);
                if (runner != null) {
                    runner.sendMessage("§aHunters will be released in §e" + timeLeft + "§a seconds!");
                }
            }
        }
        
        timeLeft--;
    }
} 