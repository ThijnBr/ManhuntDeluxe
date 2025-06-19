package com.thefallersgames.bettermanhunt.commands;

import com.thefallersgames.bettermanhunt.managers.StatsManager;
import com.thefallersgames.bettermanhunt.models.PlayerStats;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Command to display player statistics.
 */
public class StatsCommand implements CommandExecutor, TabCompleter {
    private final StatsManager statsManager;
    
    /**
     * Creates a new stats command.
     *
     * @param statsManager The stats manager to use
     */
    public StatsCommand(StatsManager statsManager) {
        this.statsManager = statsManager;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            if (sender instanceof Player) {
                // Show player's own stats
                showPlayerStats(sender, (Player) sender);
            } else {
                // Console needs to provide player name
                sender.sendMessage(ChatColor.RED + "Please specify a player name or use 'stats top <statType>'");
            }
            return true;
        }
        
        if (args[0].equalsIgnoreCase("top")) {
            if (args.length < 2) {
                // Show general help for top stats
                sender.sendMessage(ChatColor.GOLD + "=== Manhunt Top Statistics ===");
                sender.sendMessage(ChatColor.YELLOW + "Usage: /stats top <statType>");
                sender.sendMessage(ChatColor.YELLOW + "Available stat types: runnerwins, hunterwins, kills, deaths, games, dragonkills");
                return true;
            }
            
            // Show top players for specific stat
            StatsManager.StatType statType;
            try {
                statType = parseStatType(args[1]);
            } catch (IllegalArgumentException e) {
                sender.sendMessage(ChatColor.RED + "Unknown stat type: " + args[1]);
                sender.sendMessage(ChatColor.YELLOW + "Available stat types: runnerwins, hunterwins, kills, deaths, games, dragonkills");
                return true;
            }
            
            showTopStats(sender, statType, 10); // Show top 10 by default
            return true;
        }
        
        // Look up another player's stats
        Player targetPlayer = Bukkit.getPlayer(args[0]);
        if (targetPlayer == null) {
            sender.sendMessage(ChatColor.RED + "Player not found: " + args[0]);
            sender.sendMessage(ChatColor.YELLOW + "Note: You can only look up online players.");
            return true;
        }
        
        showPlayerStats(sender, targetPlayer);
        return true;
    }
    
    /**
     * Shows a player's stats to a command sender.
     *
     * @param sender The command sender to show stats to
     * @param player The player whose stats to show
     */
    private void showPlayerStats(CommandSender sender, Player player) {
        PlayerStats stats = statsManager.getPlayerStats(player);
        
        sender.sendMessage(ChatColor.GOLD + "=== " + player.getName() + "'s Manhunt Stats ===");
        sender.sendMessage(ChatColor.YELLOW + "Games played: " + ChatColor.WHITE + stats.getGamesPlayed());
        sender.sendMessage(ChatColor.BLUE + "Runner wins: " + ChatColor.WHITE + stats.getRunnerWins());
        sender.sendMessage(ChatColor.RED + "Hunter wins: " + ChatColor.WHITE + stats.getHunterWins());
        sender.sendMessage(ChatColor.GREEN + "Kills: " + ChatColor.WHITE + stats.getKills());
        sender.sendMessage(ChatColor.GRAY + "Deaths: " + ChatColor.WHITE + stats.getDeaths());
        sender.sendMessage(ChatColor.LIGHT_PURPLE + "Dragon kills: " + ChatColor.WHITE + stats.getDragonKills());
        
        // Calculate K/D ratio
        double kdRatio = stats.getDeaths() > 0 ? (double) stats.getKills() / stats.getDeaths() : stats.getKills();
        sender.sendMessage(ChatColor.AQUA + "K/D Ratio: " + ChatColor.WHITE + String.format("%.2f", kdRatio));
    }
    
    /**
     * Shows top players for a specific stat.
     *
     * @param sender The command sender to show stats to
     * @param statType The type of stat to show
     * @param limit The maximum number of players to show
     */
    private void showTopStats(CommandSender sender, StatsManager.StatType statType, int limit) {
        List<PlayerStats> topPlayers = statsManager.getTopPlayersByStatType(statType, limit);
        
        // Determine which stat name to show
        String statName;
        ChatColor statColor;
        switch (statType) {
            case RUNNER_WINS:
                statName = "Runner Wins";
                statColor = ChatColor.BLUE;
                break;
            case HUNTER_WINS:
                statName = "Hunter Wins";
                statColor = ChatColor.RED;
                break;
            case KILLS:
                statName = "Kills";
                statColor = ChatColor.GREEN;
                break;
            case DEATHS:
                statName = "Deaths";
                statColor = ChatColor.GRAY;
                break;
            case GAMES_PLAYED:
                statName = "Games Played";
                statColor = ChatColor.YELLOW;
                break;
            case DRAGON_KILLS:
                statName = "Dragon Kills";
                statColor = ChatColor.LIGHT_PURPLE;
                break;
            default:
                statName = "Stat";
                statColor = ChatColor.WHITE;
        }
        
        sender.sendMessage(ChatColor.GOLD + "=== Top " + limit + " Players by " + statName + " ===");
        
        if (topPlayers.isEmpty()) {
            sender.sendMessage(ChatColor.GRAY + "No data available yet.");
            return;
        }
        
        for (int i = 0; i < topPlayers.size(); i++) {
            PlayerStats stats = topPlayers.get(i);
            int value;
            
            // Get the appropriate stat value
            switch (statType) {
                case RUNNER_WINS:
                    value = stats.getRunnerWins();
                    break;
                case HUNTER_WINS:
                    value = stats.getHunterWins();
                    break;
                case KILLS:
                    value = stats.getKills();
                    break;
                case DEATHS:
                    value = stats.getDeaths();
                    break;
                case GAMES_PLAYED:
                    value = stats.getGamesPlayed();
                    break;
                case DRAGON_KILLS:
                    value = stats.getDragonKills();
                    break;
                default:
                    value = 0;
            }
            
            sender.sendMessage(ChatColor.GOLD + "#" + (i + 1) + " " + 
                    ChatColor.WHITE + stats.getPlayerName() + ": " + 
                    statColor + value);
        }
    }
    
    /**
     * Parses a stat type from a string.
     *
     * @param statTypeStr The string to parse
     * @return The parsed stat type
     * @throws IllegalArgumentException If the stat type is invalid
     */
    private StatsManager.StatType parseStatType(String statTypeStr) throws IllegalArgumentException {
        switch (statTypeStr.toLowerCase()) {
            case "runnerwins":
                return StatsManager.StatType.RUNNER_WINS;
            case "hunterwins":
                return StatsManager.StatType.HUNTER_WINS;
            case "kills":
                return StatsManager.StatType.KILLS;
            case "deaths":
                return StatsManager.StatType.DEATHS;
            case "games":
                return StatsManager.StatType.GAMES_PLAYED;
            case "dragonkills":
                return StatsManager.StatType.DRAGON_KILLS;
            default:
                throw new IllegalArgumentException("Invalid stat type: " + statTypeStr);
        }
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> completions = new ArrayList<>();
            completions.add("top");
            
            // Add online player names
            Bukkit.getOnlinePlayers().forEach(player -> completions.add(player.getName()));
            
            return filterCompletions(completions, args[0]);
        } else if (args.length == 2 && args[0].equalsIgnoreCase("top")) {
            List<String> statTypes = Arrays.asList(
                    "runnerwins", "hunterwins", "kills", "deaths", "games", "dragonkills");
            return filterCompletions(statTypes, args[1]);
        }
        
        return new ArrayList<>();
    }
    
    /**
     * Filters a list of completions based on the current input.
     *
     * @param completions The list of possible completions
     * @param currentInput The current input to filter by
     * @return A filtered list of completions
     */
    private List<String> filterCompletions(List<String> completions, String currentInput) {
        String lowerInput = currentInput.toLowerCase();
        return completions.stream()
                .filter(s -> s.toLowerCase().startsWith(lowerInput))
                .collect(Collectors.toList());
    }
} 