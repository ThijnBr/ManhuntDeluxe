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

/**
 * Service for managing worlds for manhunt games using Multiverse-Core API.
 */
public class WorldManagementService {
    private final Plugin plugin;
    private final Logger logger;
    private final MultiverseCoreApi coreApi;
    private final MultiverseInventoriesApi inventoriesApi;
    
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
        logger.info("Using Multiverse-Core for world management");
        
        if (inventoriesApi != null) {
            logger.info("Multiverse-Inventories detected - inventory sharing between dimensions will be enabled");
        } else {
            logger.info("Multiverse-Inventories not detected - inventory sharing between dimensions will NOT be available");
        }
    }
    
    /**
     * Creates a world using the Multiverse-Core API.
     * 
     * @param worldName The name of the world to create
     * @return A CompletableFuture containing the created world or null if creation failed
     */
    public CompletableFuture<World> createWorldWithMultiverse(String worldName) {
        CompletableFuture<World> future = new CompletableFuture<>();
        
        coreApi.getWorldManager()
            .createWorld(CreateWorldOptions.worldName(worldName)
                    .environment(World.Environment.NORMAL)
                    .seed(System.currentTimeMillis())
                    .generateStructures(true))
            .onFailure(reason -> {
                logger.warning("Failed to create world '" + worldName + "': " + reason);
                future.complete(null);
            })
            .onSuccess(multiverseWorld -> {
                logger.info("Successfully created world '" + worldName + "'");
                World bukkitWorld = Bukkit.getWorld(worldName);
                
                // Create the nether world
                String netherName = worldName + "_nether";
                coreApi.getWorldManager()
                    .createWorld(CreateWorldOptions.worldName(netherName)
                            .environment(World.Environment.NETHER)
                            .seed(System.currentTimeMillis())
                            .generateStructures(true))
                    .onFailure(reason -> logger.warning("Failed to create nether world '" + netherName + "': " + reason))
                    .onSuccess(netherWorld -> logger.info("Successfully created nether world '" + netherName + "'"));
                
                // Create the end world
                String endName = worldName + "_the_end";
                coreApi.getWorldManager()
                    .createWorld(CreateWorldOptions.worldName(endName)
                            .environment(World.Environment.THE_END)
                            .seed(System.currentTimeMillis())
                            .generateStructures(true))
                    .onFailure(reason -> logger.warning("Failed to create end world '" + endName + "': " + reason))
                    .onSuccess(endWorld -> logger.info("Successfully created end world '" + endName + "'"));
                
                future.complete(bukkitWorld);
           });

        // Group inventories to world, netherworld and endworld if MultiverseInventories is available
        if (inventoriesApi != null) {
            try {
                WorldGroup worldGroup = inventoriesApi.getWorldGroupManager().newEmptyGroup(worldName);
                // Add all three worlds to the group
                worldGroup.addWorld(worldName, false);
                worldGroup.addWorld(worldName + "_nether", false);
                worldGroup.addWorld(worldName + "_the_end", false);
                
                // Share all stats between worlds
                worldGroup.getShares().addAll(Sharables.allOf());
                
                // Update the group in the manager
                inventoriesApi.getWorldGroupManager().updateGroup(worldGroup);
                logger.info("Successfully created inventory group for world '" + worldName + "' with all stats shared");
            } catch (Exception e) {
                logger.warning("Failed to create inventory group for world '" + worldName + "': " + e.getMessage());
            }
        }
           
        return future;
    }
    
    /**
     * Deletes a world and its associated nether and end dimensions.
     *
     * @param worldName The name of the world to delete
     * @return True if the world was successfully deleted
     */
    public boolean deleteWorld(String worldName) {
        final AtomicBoolean success = new AtomicBoolean(true);
        
        // Get list of worlds to delete: main world, nether, and end
        String[] worldsToDelete = {
            worldName,
            worldName + "_nether",
            worldName + "_the_end"
        };
        
        // First remove inventory group if it exists
        if (inventoriesApi != null) {
            try {
                // Check if group exists and remove it
                WorldGroup group = inventoriesApi.getWorldGroupManager().getGroup(worldName);
                if (group != null) {
                    inventoriesApi.getWorldGroupManager().removeGroup(group);
                    logger.info("Successfully removed inventory group for world '" + worldName + "'");
                }
            } catch (Exception e) {
                logger.warning("Failed to remove inventory group for world '" + worldName + "': " + e.getMessage());
                // Continue with deletion even if group removal fails
            }
        }
        
        // Delete each world
        for (String world : worldsToDelete) {
            // Get the LoadedMultiverseWorld object instead of Bukkit World
            Option<LoadedMultiverseWorld> mvWorldOpt = coreApi.getWorldManager().getLoadedWorld(world);
            
            if (mvWorldOpt.isDefined()) {
                LoadedMultiverseWorld mvWorld = mvWorldOpt.get();
                try {
                    // Use the LoadedMultiverseWorld object for unloading
                    coreApi.getWorldManager().unloadWorld(UnloadWorldOptions.world(mvWorld))
                        .onFailure(reason -> {
                            logger.warning("Failed to unload world '" + world + "': " + reason);
                            success.set(false);
                        })
                        .onSuccess(unused -> {
                            logger.info("Successfully unloaded world '" + world + "'");
                            
                            // Delete the world files once unloaded
                            coreApi.getWorldManager().deleteWorld(DeleteWorldOptions.world(mvWorld))
                                .onFailure(reason -> {
                                    logger.warning("Failed to delete world '" + world + "': " + reason);
                                    success.set(false);
                                })
                                .onSuccess(unused2 -> logger.info("Successfully deleted world '" + world + "'"));
                        });
                } catch (Exception e) {
                    logger.warning("Error while deleting world '" + world + "': " + e.getMessage());
                    success.set(false);
                }
            }
        }
        
        return success.get();
    }
} 