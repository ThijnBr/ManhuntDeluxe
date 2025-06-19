package com.thefallersgames.bettermanhunt.managers;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.thefallersgames.bettermanhunt.Plugin;
import com.thefallersgames.bettermanhunt.models.Game;
import com.thefallersgames.bettermanhunt.models.PlayerStats;
import org.bukkit.entity.Player;

import java.io.*;
import java.lang.reflect.Type;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * Manages player statistics for Manhunt games.
 */
public class StatsManager {
    private final Plugin plugin;
    private final Map<UUID, PlayerStats> playerStats;
    private final File statsFile;
    private final Gson gson;
    
    /**
     * Creates a new stats manager.
     *
     * @param plugin The plugin instance
     */
    public StatsManager(Plugin plugin) {
        this.plugin = plugin;
        this.playerStats = new ConcurrentHashMap<>();
        this.statsFile = new File(plugin.getDataFolder(), "stats.json");
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        
        // Create plugin folder if it doesn't exist
        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }
        
        // Load existing stats
        loadStats();
    }
    
    /**
     * Loads player statistics from file.
     */
    public void loadStats() {
        playerStats.clear();
        
        if (!statsFile.exists()) {
            return; // No stats file yet
        }
        
        try (Reader reader = new FileReader(statsFile)) {
            Type type = new TypeToken<Map<UUID, PlayerStats>>(){}.getType();
            Map<UUID, PlayerStats> loadedStats = gson.fromJson(reader, type);
            
            if (loadedStats != null) {
                playerStats.putAll(loadedStats);
                plugin.getLogger().info("Loaded stats for " + playerStats.size() + " players.");
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to load player stats", e);
        }
    }
    
    /**
     * Saves player statistics to file.
     */
    public void saveStats() {
        try (Writer writer = new FileWriter(statsFile)) {
            gson.toJson(playerStats, writer);
            plugin.getLogger().info("Saved stats for " + playerStats.size() + " players.");
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save player stats", e);
        }
    }
    
    /**
     * Gets or creates player stats for a specific player.
     *
     * @param player The player
     * @return The player's stats
     */
    public PlayerStats getPlayerStats(Player player) {
        UUID playerId = player.getUniqueId();
        PlayerStats stats = playerStats.get(playerId);
        
        if (stats == null) {
            stats = new PlayerStats(playerId, player.getName());
            playerStats.put(playerId, stats);
        } else {
            // Update name in case it changed
            stats.setPlayerName(player.getName());
        }
        
        return stats;
    }
    
    /**
     * Records that a player participated in a game.
     *
     * @param player The player
     */
    public void recordGamePlayed(Player player) {
        PlayerStats stats = getPlayerStats(player);
        stats.incrementGamesPlayed();
    }
    
    /**
     * Records a player death in a game.
     *
     * @param player The player who died
     */
    public void recordDeath(Player player) {
        PlayerStats stats = getPlayerStats(player);
        stats.incrementDeaths();
    }
    
    /**
     * Records a kill by a player.
     *
     * @param killer The player who made the kill
     */
    public void recordKill(Player killer) {
        PlayerStats stats = getPlayerStats(killer);
        stats.incrementKills();
    }
    
    /**
     * Records a win for a game based on who won.
     *
     * @param game The game that ended
     * @param runnersWon True if runners won, false if hunters won
     */
    public void recordGameResult(Game game, boolean runnersWon) {
        // Update stats for all players in the game
        for (UUID runnerId : game.getRunners()) {
            Player player = plugin.getServer().getPlayer(runnerId);
            if (player != null) {
                PlayerStats stats = getPlayerStats(player);
                if (runnersWon) {
                    stats.incrementRunnerWins();
                }
            }
        }
        
        for (UUID hunterId : game.getHunters()) {
            Player player = plugin.getServer().getPlayer(hunterId);
            if (player != null) {
                PlayerStats stats = getPlayerStats(player);
                if (!runnersWon) {
                    stats.incrementHunterWins();
                }
            }
        }
        
        // Save stats after recording game results
        saveStats();
    }
    
    /**
     * Records a dragon kill by a player.
     *
     * @param player The player who killed the dragon
     */
    public void recordDragonKill(Player player) {
        PlayerStats stats = getPlayerStats(player);
        stats.incrementDragonKills();
    }
    
    /**
     * Gets the top players by a specific stat.
     *
     * @param statType The type of stat to sort by
     * @param limit The maximum number of players to return
     * @return A list of player stats sorted by the specified stat
     */
    public List<PlayerStats> getTopPlayersByStatType(StatType statType, int limit) {
        List<PlayerStats> allStats = new ArrayList<>(playerStats.values());
        
        allStats.sort((s1, s2) -> {
            int value1, value2;
            
            switch (statType) {
                case RUNNER_WINS:
                    value1 = s1.getRunnerWins();
                    value2 = s2.getRunnerWins();
                    break;
                case HUNTER_WINS:
                    value1 = s1.getHunterWins();
                    value2 = s2.getHunterWins();
                    break;
                case KILLS:
                    value1 = s1.getKills();
                    value2 = s2.getKills();
                    break;
                case DEATHS:
                    value1 = s1.getDeaths();
                    value2 = s2.getDeaths();
                    break;
                case GAMES_PLAYED:
                    value1 = s1.getGamesPlayed();
                    value2 = s2.getGamesPlayed();
                    break;
                case DRAGON_KILLS:
                    value1 = s1.getDragonKills();
                    value2 = s2.getDragonKills();
                    break;
                default:
                    value1 = 0;
                    value2 = 0;
            }
            
            return Integer.compare(value2, value1); // Descending order
        });
        
        return allStats.size() > limit ? allStats.subList(0, limit) : allStats;
    }
    
    /**
     * Enum for different types of stats.
     */
    public enum StatType {
        RUNNER_WINS,
        HUNTER_WINS,
        KILLS,
        DEATHS,
        GAMES_PLAYED,
        DRAGON_KILLS
    }
} 