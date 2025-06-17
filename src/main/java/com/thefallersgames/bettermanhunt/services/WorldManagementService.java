package com.thefallersgames.bettermanhunt.services;

import com.thefallersgames.bettermanhunt.Plugin;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.WorldType;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

/**
 * Service for managing worlds for manhunt games.
 * Will use Multiverse-Core if available, otherwise falls back to Bukkit API.
 */
public class WorldManagementService {
    private final Plugin plugin;
    private final Logger logger;
    private boolean multiverseAvailable = false;
    private Object mvCore = null; // Using Object type to avoid direct dependency

    /**
     * Constructs a new WorldManagementService.
     *
     * @param plugin The plugin instance
     */
    public WorldManagementService(Plugin plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        
        // Check if Multiverse-Core is available without directly depending on it
        try {
            if (Bukkit.getPluginManager().getPlugin("Multiverse-Core") != null) {
                Class<?> mvCoreClass = Class.forName("com.onarandombox.MultiverseCore.MultiverseCore");
                mvCore = Bukkit.getPluginManager().getPlugin("Multiverse-Core");
                multiverseAvailable = true;
                logger.info("Successfully hooked into Multiverse-Core");
            } else {
                logger.warning("Multiverse-Core not found. Using Bukkit API for world management instead.");
            }
        } catch (ClassNotFoundException e) {
            logger.warning("Multiverse-Core classes not found. Using Bukkit API for world management instead.");
        } catch (Exception e) {
            logger.warning("Error hooking into Multiverse-Core: " + e.getMessage());
            logger.warning("Using Bukkit API for world management instead.");
        }
    }
    
    /**
     * Asynchronously generates a new world for a game.
     * 
     * @param gameId A unique identifier for the game, used as part of the world name
     * @return A future that completes with the created world, or null if creation failed
     */
    public CompletableFuture<World> generateWorldAsync(String gameId) {
        CompletableFuture<World> future = new CompletableFuture<>();
        
        // Generate a unique name for this world
        String worldName = "manhunt_" + gameId + "_" + UUID.randomUUID().toString().substring(0, 8);
        
        // Run world creation on main thread but return future immediately
        Bukkit.getScheduler().runTask(plugin, () -> {
            try {
                if (multiverseAvailable) {
                    createWorldWithMultiverse(worldName, future);
                } else {
                    createWorldWithBukkit(worldName, future);
                }
            } catch (Exception e) {
                logger.severe("Error creating world: " + e.getMessage());
                e.printStackTrace();
                future.completeExceptionally(e);
            }
        });
        
        return future;
    }
    
    /**
     * Creates a world using Multiverse-Core API via reflection.
     */
    private void createWorldWithMultiverse(String worldName, CompletableFuture<World> future) {
        try {
            // Get the MVWorldManager using reflection
            Object worldManager = mvCore.getClass().getMethod("getMVWorldManager").invoke(mvCore);
            
            // Create the main world
            boolean success = (boolean) worldManager.getClass().getMethod(
                "addWorld", 
                String.class, 
                World.Environment.class,
                String.class,
                WorldType.class,
                Boolean.TYPE,
                String.class
            ).invoke(
                worldManager,
                worldName, 
                World.Environment.NORMAL, 
                null,  // seed (null = random)
                WorldType.NORMAL, 
                true,  // generate structures
                null   // generator (null = default)
            );
            
            if (!success) {
                logger.severe("Failed to create world with Multiverse: " + worldName);
                future.complete(null);
                return;
            }
            
            // Also create the nether
            worldManager.getClass().getMethod(
                "addWorld", 
                String.class, 
                World.Environment.class,
                String.class,
                WorldType.class,
                Boolean.TYPE,
                String.class
            ).invoke(
                worldManager,
                worldName + "_nether", 
                World.Environment.NETHER, 
                null, 
                WorldType.NORMAL, 
                true, 
                null
            );
            
            // And the end
            worldManager.getClass().getMethod(
                "addWorld", 
                String.class, 
                World.Environment.class,
                String.class,
                WorldType.class,
                Boolean.TYPE,
                String.class
            ).invoke(
                worldManager,
                worldName + "_the_end", 
                World.Environment.THE_END, 
                null, 
                WorldType.NORMAL, 
                true, 
                null
            );
            
            World world = Bukkit.getWorld(worldName);
            future.complete(world);
            logger.info("Successfully created world with Multiverse: " + worldName + " with nether and end dimensions");
            
        } catch (Exception e) {
            logger.severe("Error using Multiverse to create world: " + e.getMessage());
            e.printStackTrace();
            
            // Fall back to Bukkit method if Multiverse fails
            logger.info("Falling back to Bukkit API for world creation");
            createWorldWithBukkit(worldName, future);
        }
    }
    
    /**
     * Creates a world using the standard Bukkit API.
     */
    private void createWorldWithBukkit(String worldName, CompletableFuture<World> future) {
        try {
            // Create the main world
            WorldCreator creator = new WorldCreator(worldName);
            creator.environment(World.Environment.NORMAL);
            creator.type(WorldType.NORMAL);
            creator.generateStructures(true);
            
            World world = Bukkit.createWorld(creator);
            
            if (world == null) {
                logger.severe("Failed to create world with Bukkit: " + worldName);
                future.complete(null);
                return;
            }
            
            // Also create the nether
            WorldCreator netherCreator = new WorldCreator(worldName + "_nether");
            netherCreator.environment(World.Environment.NETHER);
            netherCreator.type(WorldType.NORMAL);
            netherCreator.generateStructures(true);
            Bukkit.createWorld(netherCreator);
            
            // And the end
            WorldCreator endCreator = new WorldCreator(worldName + "_the_end");
            endCreator.environment(World.Environment.THE_END);
            endCreator.type(WorldType.NORMAL);
            endCreator.generateStructures(true);
            Bukkit.createWorld(endCreator);
            
            future.complete(world);
            logger.info("Successfully created world with Bukkit API: " + worldName + " with nether and end dimensions");
            
        } catch (Exception e) {
            logger.severe("Error creating world with Bukkit API: " + e.getMessage());
            e.printStackTrace();
            future.completeExceptionally(e);
        }
    }
    
    /**
     * Deletes a world and its associated nether and end dimensions.
     *
     * @param worldName The name of the world to delete
     * @return True if the world was successfully deleted
     */
    public boolean deleteWorld(String worldName) {
        try {
            if (multiverseAvailable) {
                return deleteWorldWithMultiverse(worldName);
            } else {
                return deleteWorldWithBukkit(worldName);
            }
        } catch (Exception e) {
            logger.severe("Error deleting world: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Deletes a world using Multiverse-Core API via reflection.
     */
    private boolean deleteWorldWithMultiverse(String worldName) throws Exception {
        // Get the MVWorldManager using reflection
        Object worldManager = mvCore.getClass().getMethod("getMVWorldManager").invoke(mvCore);
        
        // First check if the world exists in Multiverse
        boolean worldExists = (boolean) worldManager.getClass().getMethod("isMVWorld", String.class).invoke(worldManager, worldName);
        
        if (worldExists) {
            // Remove all players from the world first
            World world = Bukkit.getWorld(worldName);
            if (world != null) {
                World defaultWorld = Bukkit.getWorlds().get(0);
                for (Player player : world.getPlayers()) {
                    player.teleport(defaultWorld.getSpawnLocation());
                }
            }
            
            // Delete nether and end dimensions if they exist
            if ((boolean) worldManager.getClass().getMethod("isMVWorld", String.class)
                    .invoke(worldManager, worldName + "_nether")) {
                worldManager.getClass().getMethod("deleteWorld", String.class)
                    .invoke(worldManager, worldName + "_nether");
            }
            
            if ((boolean) worldManager.getClass().getMethod("isMVWorld", String.class)
                    .invoke(worldManager, worldName + "_the_end")) {
                worldManager.getClass().getMethod("deleteWorld", String.class)
                    .invoke(worldManager, worldName + "_the_end");
            }
            
            // Delete the main world
            boolean success = (boolean) worldManager.getClass().getMethod("deleteWorld", String.class)
                .invoke(worldManager, worldName);
                
            if (success) {
                logger.info("Successfully deleted world with Multiverse: " + worldName);
            } else {
                logger.warning("Failed to delete world with Multiverse: " + worldName);
            }
            return success;
        }
        
        return false;
    }
    
    /**
     * Deletes a world using Bukkit API.
     * Note: Bukkit doesn't have a built-in world deletion method, so this is a basic implementation.
     */
    private boolean deleteWorldWithBukkit(String worldName) {
        try {
            // First unload the worlds
            unloadWorld(worldName);
            unloadWorld(worldName + "_nether");
            unloadWorld(worldName + "_the_end");
            
            // Then delete the world directories
            boolean mainDeleted = deleteWorldDirectory(worldName);
            boolean netherDeleted = deleteWorldDirectory(worldName + "_nether");
            boolean endDeleted = deleteWorldDirectory(worldName + "_the_end");
            
            if (mainDeleted) {
                logger.info("Successfully deleted world with Bukkit API: " + worldName);
            } else {
                logger.warning("Failed to delete world with Bukkit API: " + worldName);
            }
            
            return mainDeleted;
        } catch (Exception e) {
            logger.severe("Error deleting world with Bukkit API: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Unloads a world safely, teleporting players first if needed.
     */
    private boolean unloadWorld(String worldName) {
        World world = Bukkit.getWorld(worldName);
        if (world != null) {
            // Teleport all players out of the world
            World defaultWorld = Bukkit.getWorlds().get(0);
            for (Player player : world.getPlayers()) {
                player.teleport(defaultWorld.getSpawnLocation());
            }
            
            // Unload the world
            return Bukkit.unloadWorld(world, false);
        }
        return true; // World doesn't exist, so consider it "unloaded"
    }
    
    /**
     * Deletes a world directory.
     */
    private boolean deleteWorldDirectory(String worldName) {
        File worldFolder = new File(Bukkit.getWorldContainer(), worldName);
        if (worldFolder.exists() && worldFolder.isDirectory()) {
            return deleteDirectory(worldFolder);
        }
        return true; // Directory doesn't exist, so consider it "deleted"
    }
    
    /**
     * Recursively deletes a directory.
     */
    private boolean deleteDirectory(File directory) {
        if (directory.exists()) {
            File[] files = directory.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        deleteDirectory(file);
                    } else {
                        file.delete();
                    }
                }
            }
        }
        return directory.delete();
    }
    
    /**
     * Checks if Multiverse is properly loaded and available.
     *
     * @return True if Multiverse is available
     */
    public boolean isMultiverseAvailable() {
        return multiverseAvailable;
    }

    /**
     * Checks if a world is accessible for teleportation.
     *
     * @param world The world to check
     * @param player The player to check teleportation for (optional, can be null)
     * @return True if the world is accessible, false otherwise
     */
    public boolean isWorldAccessibleForTeleport(World world, Player player) {
        if (world == null) {
            logger.warning("Cannot check accessibility of null world");
            return false;
        }
        
        try {
            // Check if world is loaded
            if (!world.isChunkLoaded(0, 0)) {
                // Try to load the chunk
                boolean loaded = world.loadChunk(0, 0, true);
                if (!loaded) {
                    logger.warning("Could not load spawn chunk for world: " + world.getName());
                    return false;
                }
            }
            
            // If player is provided, do a test teleport
            if (player != null) {
                // Store current location
                Location currentLocation = player.getLocation().clone();
                
                // Try teleporting to world spawn and back
                boolean canTeleport = player.teleport(world.getSpawnLocation());
                
                // Teleport back to original location
                player.teleport(currentLocation);
                
                return canTeleport;
            }
            
            return true;
        } catch (Exception e) {
            logger.warning("Error checking world accessibility: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
} 