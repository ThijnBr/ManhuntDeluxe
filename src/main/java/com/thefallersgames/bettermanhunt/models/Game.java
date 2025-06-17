package com.thefallersgames.bettermanhunt.models;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Represents a Manhunt game instance with its state and participants.
 */
public class Game {
    private final String name;
    private UUID owner;  // Changed from final to allow owner changes
    private final World world;
    private GameState state;
    private final Set<UUID> hunters;
    private final Set<UUID> runners;
    private final Set<UUID> spectators;
    private int headstartDuration; // in seconds
    private Location spawnLocation;

    /**
     * Creates a new Manhunt game.
     *
     * @param name The name of the game
     * @param owner The player who created the game
     * @param world The world where the game is taking place
     */
    public Game(String name, Player owner, World world) {
        this.name = name;
        this.owner = owner.getUniqueId();
        this.world = world;
        this.state = GameState.LOBBY;
        this.hunters = new HashSet<>();
        this.runners = new HashSet<>();
        this.spectators = new HashSet<>();
        this.headstartDuration = 30; // Default headstart of 30 seconds
        this.spawnLocation = world.getSpawnLocation().clone(); // Use the world's spawn location
    }

    /**
     * Adds a player to the hunters team.
     *
     * @param player The player to add
     * @return True if the player was added, false if already in the team
     */
    public boolean addHunter(Player player) {
        UUID playerId = player.getUniqueId();
        runners.remove(playerId);
        spectators.remove(playerId);
        return hunters.add(playerId);
    }

    /**
     * Adds a player to the runners team.
     *
     * @param player The player to add
     * @return True if the player was added, false if already in the team
     */
    public boolean addRunner(Player player) {
        UUID playerId = player.getUniqueId();
        hunters.remove(playerId);
        spectators.remove(playerId);
        return runners.add(playerId);
    }

    /**
     * Adds a player to the spectators.
     *
     * @param player The player to add
     * @return True if the player was added, false if already in the team
     */
    public boolean addSpectator(Player player) {
        UUID playerId = player.getUniqueId();
        hunters.remove(playerId);
        runners.remove(playerId);
        return spectators.add(playerId);
    }

    /**
     * Removes a player from all teams in the game.
     *
     * @param player The player to remove
     */
    public void removePlayer(Player player) {
        UUID playerId = player.getUniqueId();
        hunters.remove(playerId);
        runners.remove(playerId);
        spectators.remove(playerId);
    }

    /**
     * Checks if the player is part of this game.
     *
     * @param player The player to check
     * @return True if the player is in any team in this game
     */
    public boolean isPlayerInGame(Player player) {
        UUID playerId = player.getUniqueId();
        return hunters.contains(playerId) || runners.contains(playerId) || spectators.contains(playerId);
    }

    /**
     * Checks if the player is a hunter.
     *
     * @param player The player to check
     * @return True if the player is a hunter
     */
    public boolean isHunter(Player player) {
        return hunters.contains(player.getUniqueId());
    }

    /**
     * Checks if the player is a runner.
     *
     * @param player The player to check
     * @return True if the player is a runner
     */
    public boolean isRunner(Player player) {
        return runners.contains(player.getUniqueId());
    }

    /**
     * Checks if the player is a spectator.
     *
     * @param player The player to check
     * @return True if the player is a spectator
     */
    public boolean isSpectator(Player player) {
        return spectators.contains(player.getUniqueId());
    }

    /**
     * Checks if the player is the game owner.
     *
     * @param player The player to check
     * @return True if the player is the owner
     */
    public boolean isOwner(Player player) {
        return owner.equals(player.getUniqueId());
    }

    /**
     * Gets the name of the game.
     *
     * @return The game name
     */
    public String getName() {
        return name;
    }

    /**
     * Gets the current state of the game.
     *
     * @return The current game state
     */
    public GameState getState() {
        return state;
    }

    /**
     * Sets the state of the game.
     *
     * @param state The new game state
     */
    public void setState(GameState state) {
        this.state = state;
    }

    /**
     * Gets the world where the game is taking place.
     *
     * @return The game world
     */
    public World getWorld() {
        return world;
    }

    /**
     * Gets the spawn location for the game.
     *
     * @return The spawn location
     */
    public Location getSpawnLocation() {
        return spawnLocation.clone();
    }

    /**
     * Sets the spawn location for the game.
     *
     * @param spawnLocation The new spawn location
     */
    public void setSpawnLocation(Location spawnLocation) {
        this.spawnLocation = spawnLocation.clone();
    }

    /**
     * Gets the headstart duration in seconds.
     *
     * @return The headstart duration
     */
    public int getHeadstartDuration() {
        return headstartDuration;
    }

    /**
     * Sets the headstart duration in seconds.
     *
     * @param headstartDuration The new headstart duration
     */
    public void setHeadstartDuration(int headstartDuration) {
        this.headstartDuration = headstartDuration;
    }

    /**
     * Gets the set of hunter UUIDs.
     *
     * @return The set of hunter UUIDs
     */
    public Set<UUID> getHunters() {
        return new HashSet<>(hunters);
    }

    /**
     * Gets the set of runner UUIDs.
     *
     * @return The set of runner UUIDs
     */
    public Set<UUID> getRunners() {
        return new HashSet<>(runners);
    }

    /**
     * Gets the set of spectator UUIDs.
     *
     * @return The set of spectator UUIDs
     */
    public Set<UUID> getSpectators() {
        return new HashSet<>(spectators);
    }

    /**
     * Gets the UUID of the game owner.
     *
     * @return The owner's UUID
     */
    public UUID getOwner() {
        return owner;
    }

    /**
     * Changes the owner of the game.
     * 
     * @param newOwner The player who will become the new owner
     */
    public void setOwner(Player newOwner) {
        this.owner = newOwner.getUniqueId();
    }

    /**
     * Gets all player UUIDs in the game.
     *
     * @return A set containing all player UUIDs
     */
    public Set<UUID> getAllPlayers() {
        Set<UUID> allPlayers = new HashSet<>();
        allPlayers.addAll(hunters);
        allPlayers.addAll(runners);
        allPlayers.addAll(spectators);
        return allPlayers;
    }
} 