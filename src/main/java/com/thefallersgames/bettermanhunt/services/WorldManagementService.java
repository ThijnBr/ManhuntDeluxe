package com.thefallersgames.bettermanhunt.services;

import com.thefallersgames.bettermanhunt.Plugin;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.WorldType;
import org.bukkit.entity.Player;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import org.mvplugins.multiverse.core.MultiverseCoreApi;
import org.mvplugins.multiverse.core.world.options.CreateWorldOptions;
import org.mvplugins.multiverse.core.world.options.UnloadWorldOptions;
import org.mvplugins.multiverse.core.world.options.DeleteWorldOptions;
import org.mvplugins.multiverse.core.world.LoadedMultiverseWorld;
import org.mvplugins.multiverse.inventories.MultiverseInventoriesApi;
import org.mvplugins.multiverse.inventories.profile.group.WorldGroup;
import org.mvplugins.multiverse.inventories.share.Sharables;
import org.mvplugins.multiverse.external.vavr.control.Option;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Service for managing worlds for manhunt games using Multiverse-Core API.
 */
public class WorldManagementService {
    private final Plugin plugin;
    private final Logger logger;
    private final MultiverseCoreApi coreApi;
    private final MultiverseInventoriesApi inventoriesApi;
    private final String worldsFolder;
    
    /**
     * Constructs a new WorldManagementService.
     *
     * @param plugin The plugin instance
     * @param coreApi The MultiverseCore API instance
     * @param inventoriesApi The MultiverseInventories API instance (can be null if not available)
     */
    public WorldManagementService(Plugin plugin, MultiverseCoreApi coreApi, MultiverseInventoriesApi inventoriesApi) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        this.coreApi = coreApi;
        this.inventoriesApi = inventoriesApi;
        this.worldsFolder = plugin.getConfig().getString("custom-worlds-folder", "ManhuntWorlds");
        logger.info("Using Multiverse-Core for world management");
        logger.info("Custom worlds will be stored in folder: " + worldsFolder);
        
        if (inventoriesApi != null) {
            logger.info("Multiverse-Inventories detected - inventory sharing between dimensions will be enabled");
        } else {
            logger.info("Multiverse-Inventories not detected - inventory sharing between dimensions will NOT be available");
        }
    }
    
    /**
     * Creates a world using the Multiverse-Core API.
     * Worlds are stored in a ManhuntWorlds folder to keep them organized.
     * This runs asynchronously to avoid blocking the main thread during world generation.
     * 
     * @param worldName The name of the world to create
     * @return A CompletableFuture containing the created world or null if creation failed
     */
    public CompletableFuture<World> createWorldWithMultiverse(String worldName) {
        CompletableFuture<World> future = new CompletableFuture<>();
        
        // Run the world creation asynchronously
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                // Put all manhunt worlds in a dedicated folder
                String folderWorldName = worldsFolder + "/" + worldName;
                
                logger.info("Starting async creation of world '" + folderWorldName + "'");
                
                // We need to run the actual world creation on the main thread
                // but we can schedule it from our async thread
                Bukkit.getScheduler().runTask(plugin, () -> {
                    coreApi.getWorldManager()
                        .createWorld(CreateWorldOptions.worldName(folderWorldName)
                                .environment(World.Environment.NORMAL)
                                .seed(System.currentTimeMillis())
                                .generateStructures(true))
                        .onFailure(reason -> {
                            logger.warning("Failed to create world '" + folderWorldName + "': " + reason);
                            future.complete(null);
                        })
                        .onSuccess(multiverseWorld -> {
                            logger.info("Successfully created world '" + folderWorldName + "'");
                            World bukkitWorld = Bukkit.getWorld(folderWorldName);
                            
                            // Create nether world after a short delay to avoid hammering the server
                            Bukkit.getScheduler().runTaskLater(plugin, () -> createNetherWorld(folderWorldName, 
                                () -> createEndWorld(folderWorldName, () -> createWorldGroup(folderWorldName))), 20L);
                            
                            // Complete the future immediately so the player can start playing
                            future.complete(bukkitWorld);
                        });
                });
            } catch (Exception e) {
                logger.severe("Error during async world creation: " + e.getMessage());
                future.completeExceptionally(e);
            }
        });
        
        return future;
    }
    
    /**
     * Creates a nether world for the given base world.
     * 
     * @param baseWorldName The base world name (including folder)
     * @param onComplete Runnable to execute when complete
     */
    private void createNetherWorld(String baseWorldName, Runnable onComplete) {
        String netherName = baseWorldName + "_nether";
        logger.info("Creating nether world '" + netherName + "'");
        
        coreApi.getWorldManager()
            .createWorld(CreateWorldOptions.worldName(netherName)
                    .environment(World.Environment.NETHER)
                    .seed(System.currentTimeMillis())
                    .generateStructures(true))
            .onFailure(reason -> {
                logger.warning("Failed to create nether world '" + netherName + "': " + reason);
                if (onComplete != null) onComplete.run();
            })
            .onSuccess(netherWorld -> {
                logger.info("Successfully created nether world '" + netherName + "'");
                if (onComplete != null) onComplete.run();
            });
    }
    
    /**
     * Creates an end world for the given base world.
     * 
     * @param baseWorldName The base world name (including folder)
     * @param onComplete Runnable to execute when complete
     */
    private void createEndWorld(String baseWorldName, Runnable onComplete) {
        String endName = baseWorldName + "_the_end";
        logger.info("Creating end world '" + endName + "'");
        
        coreApi.getWorldManager()
            .createWorld(CreateWorldOptions.worldName(endName)
                    .environment(World.Environment.THE_END)
                    .seed(System.currentTimeMillis())
                    .generateStructures(true))
            .onFailure(reason -> {
                logger.warning("Failed to create end world '" + endName + "': " + reason);
                if (onComplete != null) onComplete.run();
            })
            .onSuccess(endWorld -> {
                logger.info("Successfully created end world '" + endName + "'");
                if (onComplete != null) onComplete.run();
            });
    }
    
    /**
     * Creates an inventory world group if MultiverseInventories is available.
     * 
     * @param baseWorldName The base world name (including folder)
     */
    private void createWorldGroup(String baseWorldName) {
        // Group inventories to world, netherworld and endworld if MultiverseInventories is available
        if (inventoriesApi != null) {
            try {
                WorldGroup worldGroup = inventoriesApi.getWorldGroupManager().newEmptyGroup(baseWorldName);
                // Add all three worlds to the group
                worldGroup.addWorld(baseWorldName, false);
                worldGroup.addWorld(baseWorldName + "_nether", false);
                worldGroup.addWorld(baseWorldName + "_the_end", false);
                
                // Share all stats between worlds
                worldGroup.getShares().addAll(Sharables.allOf());
                
                // Update the group in the manager
                inventoriesApi.getWorldGroupManager().updateGroup(worldGroup);
                logger.info("Successfully created inventory group for world '" + baseWorldName + "' with all stats shared");
            } catch (Exception e) {
                logger.warning("Failed to create inventory group for world '" + baseWorldName + "': " + e.getMessage());
            }
        }
    }
    
    /**
     * Deletes a world and its associated nether and end dimensions.
     * If the worldName doesn't include a folder prefix, assumes it's in the ManhuntWorlds folder.
     * This operation coordinates between async and sync operations to avoid blocking the main thread unnecessarily.
     *
     * @param worldName The name of the world to delete
     * @return CompletableFuture that completes with true if the world was successfully deleted
     */
    public CompletableFuture<Boolean> deleteWorld(String worldName) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        
        // Run preparation asynchronously
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                final AtomicBoolean success = new AtomicBoolean(true);
                
                // If the world name doesn't have a folder prefix, assume it's in ManhuntWorlds folder
                final String finalWorldName;
                if (!worldName.contains("/")) {
                    finalWorldName = worldsFolder + "/" + worldName;
                } else {
                    finalWorldName = worldName;
                }
                
                // Get list of worlds to delete: main world, nether, and end
                String[] worldsToDelete = {
                    finalWorldName,
                    finalWorldName + "_nether",
                    finalWorldName + "_the_end"
                };
                
                // First remove inventory group if it exists (can be done async)
                if (inventoriesApi != null) {
                    try {
                        // Check if group exists and remove it
                        WorldGroup group = inventoriesApi.getWorldGroupManager().getGroup(finalWorldName);
                        if (group != null) {
                            inventoriesApi.getWorldGroupManager().removeGroup(group);
                            logger.info("Successfully removed inventory group for world '" + finalWorldName + "'");
                        }
                    } catch (Exception e) {
                        logger.warning("Failed to remove inventory group for world '" + finalWorldName + "': " + e.getMessage());
                        // Continue with deletion even if group removal fails
                    }
                }
                
                // World unloading needs to be done on the main thread
                Bukkit.getScheduler().runTask(plugin, () -> {
                    // First unload all worlds
                    try {
                        unloadWorlds(worldsToDelete, success, () -> {
                            // Then delete all worlds (also needs to be done on the main thread for certain API operations)
                            Bukkit.getScheduler().runTask(plugin, () -> {
                                try {
                                    deleteUnloadedWorlds(worldsToDelete, success);
                                    future.complete(success.get());
                                } catch (Exception e) {
                                    logger.severe("Error during world deletion: " + e.getMessage());
                                    future.complete(false);
                                }
                            });
                        });
                    } catch (Exception e) {
                        logger.severe("Error during world unloading: " + e.getMessage());
                        future.complete(false);
                    }
                });
            } catch (Exception e) {
                logger.severe("Error initiating world deletion: " + e.getMessage());
                future.complete(false);
            }
        });
        
        return future;
    }
    
    /**
     * Unloads worlds sequentially on the main thread.
     * 
     * @param worldsToUnload Array of world names to unload
     * @param success AtomicBoolean that tracks success
     * @param onComplete Runnable to execute when all worlds are unloaded
     */
    private void unloadWorlds(String[] worldsToUnload, AtomicBoolean success, Runnable onComplete) {
        if (worldsToUnload.length == 0) {
            if (onComplete != null) onComplete.run();
            return;
        }
        
        String[] remainingWorlds = new String[worldsToUnload.length - 1];
        System.arraycopy(worldsToUnload, 1, remainingWorlds, 0, remainingWorlds.length);
        
        // Get the world to unload
        String worldToUnload = worldsToUnload[0];
        Option<LoadedMultiverseWorld> mvWorldOpt = coreApi.getWorldManager().getLoadedWorld(worldToUnload);
        
        if (mvWorldOpt.isDefined()) {
            LoadedMultiverseWorld mvWorld = mvWorldOpt.get();
            // Use the LoadedMultiverseWorld object for unloading
            coreApi.getWorldManager().unloadWorld(UnloadWorldOptions.world(mvWorld))
                .onFailure(reason -> {
                    logger.warning("Failed to unload world '" + worldToUnload + "': " + reason);
                    success.set(false);
                    
                    // Continue with remaining worlds
                    unloadWorlds(remainingWorlds, success, onComplete);
                })
                .onSuccess(unused -> {
                    logger.info("Successfully unloaded world '" + worldToUnload + "'");
                    
                    // Process remaining worlds
                    unloadWorlds(remainingWorlds, success, onComplete);
                });
        } else {
            logger.warning("World '" + worldToUnload + "' not found, skipping unload");
            // Process remaining worlds
            unloadWorlds(remainingWorlds, success, onComplete);
        }
    }
    
    /**
     * Deletes unloaded worlds.
     * This method must be called on the main server thread.
     * 
     * @param worldsToDelete Array of world names to delete
     * @param success AtomicBoolean that tracks success
     */
    private void deleteUnloadedWorlds(String[] worldsToDelete, AtomicBoolean success) {
        // This method must be running on the main thread
        if (!Bukkit.isPrimaryThread()) {
            logger.severe("deleteUnloadedWorlds must be called from the main thread");
            success.set(false);
            return;
        }

        for (String world : worldsToDelete) {
            // Use MultiverseWorld instead of LoadedMultiverseWorld for unloaded worlds
            Option<org.mvplugins.multiverse.core.world.MultiverseWorld> mvWorldOpt = coreApi.getWorldManager().getUnloadedWorld(world);
            
            if (mvWorldOpt.isDefined()) {
                org.mvplugins.multiverse.core.world.MultiverseWorld mvWorld = mvWorldOpt.get();
                try {
                    // Delete the world files
                    coreApi.getWorldManager().deleteWorld(DeleteWorldOptions.world(mvWorld))
                        .onFailure(reason -> {
                            logger.warning("Failed to delete world '" + world + "': " + reason);
                            success.set(false);
                        })
                        .onSuccess(unused -> logger.info("Successfully deleted world '" + world + "'"));
                } catch (Exception e) {
                    logger.warning("Error while deleting world '" + world + "': " + e.getMessage());
                    e.printStackTrace();
                    success.set(false);
                }
            }
        }
    }
    
    /**
     * Scans for and cleans up all worlds in the ManhuntWorlds folder.
     * This is used during plugin startup and shutdown to ensure no stale worlds are kept.
     * 
     * @param synchronous If true, will wait for all deletions to complete
     */
    public void cleanupAllManhuntWorlds(boolean synchronous) {
        if (synchronous) {
            cleanupAllManhuntWorldsSync();
        } else {
            cleanupAllManhuntWorldsAsync();
        }
    }

    private void cleanupAllManhuntWorldsAsync() {
        logger.info("Cleaning up ALL Manhunt worlds asynchronously...");

        // Locate the worldsFolder
        File worldContainer = Bukkit.getServer().getWorldContainer();
        File manhuntFolder = new File(worldContainer, worldsFolder);
        if (!manhuntFolder.exists() || !manhuntFolder.isDirectory()) {
            logger.info("No ManhuntWorlds folder found. Nothing to clean up.");
            return;
        }

        // List all items in the folder
        File[] entries = manhuntFolder.listFiles();
        if (entries == null || entries.length == 0) {
            logger.info("ManhuntWorlds folder is empty. Nothing to delete.");
            return;
        }

        List<File> worldFolders = new ArrayList<>(Arrays.asList(entries));

        // The actual cleanup logic needs to run on the main thread for API calls.
        // We can start it from an async task to not block startup/shutdown.
        Runnable cleanupTask = () -> processWorldCleanup(worldFolders);

        Bukkit.getScheduler().runTaskAsynchronously(plugin, cleanupTask);
    }

    private void cleanupAllManhuntWorldsSync() {
        logger.info("Cleaning up ALL Manhunt worlds synchronously...");

        // Locate the worldsFolder
        File worldContainer = Bukkit.getServer().getWorldContainer();
        File manhuntFolder = new File(worldContainer, worldsFolder);
        if (!manhuntFolder.exists() || !manhuntFolder.isDirectory()) {
            logger.info("No ManhuntWorlds folder found. Nothing to clean up.");
            return;
        }

        // List all items in the folder
        File[] entries = manhuntFolder.listFiles();
        if (entries == null || entries.length == 0) {
            logger.info("ManhuntWorlds folder is empty. Nothing to delete.");
            return;
        }

        for (File worldFolder : entries) {
            String worldFolderName = worldFolder.getName();
            String fullWorldName = worldsFolder + "/" + worldFolderName;

            // First, try to unload the world if it's loaded.
            coreApi.getWorldManager().getLoadedWorld(fullWorldName).peek(loadedWorld -> {
                logger.info("Unloading world synchronously: " + fullWorldName);
                coreApi.getWorldManager().unloadWorld(UnloadWorldOptions.world(loadedWorld));
            });

            // After unloading (or failure), proceed to delete from Multiverse, which also handles files.
            coreApi.getWorldManager().getUnloadedWorld(fullWorldName).peek(mvWorld -> {
                logger.info("Deleting world synchronously: " + fullWorldName);
                coreApi.getWorldManager().deleteWorld(DeleteWorldOptions.world(mvWorld));
            });

            // Also try to remove inventory group
            if (inventoriesApi != null && !worldFolderName.endsWith("_nether") && !worldFolderName.endsWith("_the_end")) {
                WorldGroup group = inventoriesApi.getWorldGroupManager().getGroup(fullWorldName);
                if (group != null) {
                    inventoriesApi.getWorldGroupManager().removeGroup(group);
                    logger.info("Successfully removed inventory group for world '" + fullWorldName + "'");
                }
            }
        }
        logger.info("Synchronous cleanup of Manhunt worlds complete.");
    }
 
    /**
     * Processes the list of world folders to clean up, one by one.
     * This method is designed to be called recursively in callbacks to ensure sequential execution.
     */
    private void processWorldCleanup(List<File> worldFolders) {
        if (worldFolders.isEmpty()) {
            logger.info("All Manhunt worlds have been cleaned up.");
            return;
        }
 
        File worldFolder = worldFolders.remove(0);
        String worldFolderName = worldFolder.getName();
        String fullWorldName = worldsFolder + "/" + worldFolderName;
 
        // Schedule the actual unload/delete on the main thread.
        Bukkit.getScheduler().runTask(plugin, () -> {
            Runnable deleteAndContinue = () -> {
                // After unloading (or failure), proceed to delete from Multiverse, which also handles files.
                coreApi.getWorldManager().getUnloadedWorld(fullWorldName).peek(mvWorld -> {
                    coreApi.getWorldManager().deleteWorld(DeleteWorldOptions.world(mvWorld))
                        .onSuccess(v -> logger.info("Successfully deleted world: " + fullWorldName))
                        .onFailure(r -> logger.warning("Failed to delete world '" + fullWorldName + "': " + r));
                });
 
                // Also try to remove inventory group
                if (inventoriesApi != null && !worldFolderName.endsWith("_nether") && !worldFolderName.endsWith("_the_end")) {
                    WorldGroup group = inventoriesApi.getWorldGroupManager().getGroup(fullWorldName);
                    if (group != null) {
                        inventoriesApi.getWorldGroupManager().removeGroup(group);
                        logger.info("Successfully removed inventory group for world '" + fullWorldName + "'");
                    }
                }
 
                // Process the next world in the list
                processWorldCleanup(worldFolders);
            };
 
            // First, try to unload the world if it's loaded.
            Option<LoadedMultiverseWorld> loadedWorldOpt = coreApi.getWorldManager().getLoadedWorld(fullWorldName);
            if (loadedWorldOpt.isDefined()) {
                coreApi.getWorldManager().unloadWorld(UnloadWorldOptions.world(loadedWorldOpt.get()))
                    .onSuccess(v -> {
                        logger.info("Unloaded world: " + fullWorldName);
                        deleteAndContinue.run();
                    })
                    .onFailure(r -> {
                        logger.warning("Failed to unload world '" + fullWorldName + "': " + r + ". Forcing deletion.");
                        deleteAndContinue.run();
                    });
            } else {
                // If not loaded, just delete it.
                deleteAndContinue.run();
            }
        });
    }
} 