package com.thefallersgames.bettermanhunt.managers;

import com.thefallersgames.bettermanhunt.models.Game;
import com.thefallersgames.bettermanhunt.models.GameState;
import org.bukkit.entity.Player;
import org.bukkit.World;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Manages registration and tracking of games and players.
 */
public class GameRegistry {
    private final Map<String, Game> games;
    private final Map<UUID, String> playerGames;

    /**
     * Creates a new GameRegistry.
     */
    public GameRegistry() {
        this.games = new HashMap<>();
        this.playerGames = new HashMap<>();
    }

    /**
     * Checks if a game exists with the given name.
     *
     * @param gameName The name of the game
     * @return True if the game exists
     */
    public boolean gameExists(String gameName) {
        return games.containsKey(gameName);
    }

    /**
     * Registers a new game.
     *
     * @param game The game to register
     * @param owner The player who owns the game
     */
    public void registerGame(Game game, Player owner) {
        games.put(game.getName(), game);
        playerGames.put(owner.getUniqueId(), game.getName());
    }

    /**
     * Unregisters a game.
     *
     * @param gameName The name of the game to unregister
     */
    public void unregisterGame(String gameName) {
        games.remove(gameName);
    }

    /**
     * Gets a game by name.
     *
     * @param gameName The name of the game
     * @return The game, or null if it doesn't exist
     */
    public Game getGame(String gameName) {
        return games.get(gameName);
    }

    /**
     * Gets all registered games.
     *
     * @return A collection of all games
     */
    public Collection<Game> getAllGames() {
        return games.values();
    }

    /**
     * Gets all game names.
     *
     * @return A set of all game names
     */
    public Set<String> getAllGameNames() {
        return new HashSet<>(games.keySet());
    }

    /**
     * Gets the game a player is in.
     *
     * @param playerId The UUID of the player
     * @return The name of the game the player is in, or null
     */
    public String getPlayerGame(UUID playerId) {
        return playerGames.get(playerId);
    }

    /**
     * Adds a player to a game.
     *
     * @param player The player to add
     * @param game The game to add the player to
     */
    public void addPlayerToGame(Player player, Game game) {
        playerGames.put(player.getUniqueId(), game.getName());
    }

    /**
     * Removes a player from their game.
     *
     * @param playerId The UUID of the player to remove
     */
    public void removePlayerFromGame(UUID playerId) {
        playerGames.remove(playerId);
    }

    /**
     * Gets games with specific states.
     *
     * @param states The states to filter by
     * @return A list of games with the specified states
     */
    public List<Game> getGamesWithStates(GameState... states) {
        Set<GameState> stateSet = new HashSet<>(Arrays.asList(states));
        return games.values().stream()
                .filter(game -> stateSet.contains(game.getState()))
                .collect(Collectors.toList());
    }

    /**
     * Updates the owner of a game.
     *
     * @param gameName The name of the game
     * @param newOwner The player who will become the new owner
     * @return True if the owner was updated, false if the game doesn't exist
     */
    public boolean updateGameOwner(String gameName, Player newOwner) {
        Game game = games.get(gameName);
        if (game == null) {
            return false;
        }
        
        // Update the game owner directly
        game.setOwner(newOwner);
        
        return true;
    }
} 