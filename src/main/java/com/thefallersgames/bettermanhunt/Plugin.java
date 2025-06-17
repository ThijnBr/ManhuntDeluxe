package com.thefallersgames.bettermanhunt;

import com.thefallersgames.bettermanhunt.commands.*;
import com.thefallersgames.bettermanhunt.listeners.*;
import com.thefallersgames.bettermanhunt.managers.*;
import com.thefallersgames.bettermanhunt.services.GameTaskService;
import com.thefallersgames.bettermanhunt.services.LobbyService;
import com.thefallersgames.bettermanhunt.services.WorldManagementService;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Logger;

/**
 * Manhunt Deluxe main plugin class
 */
public class Plugin extends JavaPlugin {
    private static final Logger LOGGER = Logger.getLogger("manhuntdeluxe");
    private GameManager gameManager;
    private PlayerListener playerListener;
    private GuiManager guiManager;
    private TeamChatManager teamChatManager;
    private GuiListener guiListener;
    private LobbyProtectionListener lobbyProtectionListener;
    private GameItemProtectionListener gameItemProtectionListener;
    private HeadstartListener headstartListener;
    private WorldManagementService worldManagementService;
    private LobbyService lobbyService;
    private GameTaskService gameTaskService;
    private HeadstartManager headstartManager;

    @Override
    public void onEnable() {
        // Create config if it doesn't exist
        saveDefaultConfig();
        
        // Initialize services
        worldManagementService = new WorldManagementService(this);
        lobbyService = new LobbyService(this);
        headstartManager = new HeadstartManager();
        
        // Initialize GameTaskService with a supplier to avoid circular dependency
        gameTaskService = new GameTaskService(this, () -> gameManager, headstartManager);
        
        // Initialize managers
        gameManager = new GameManager(this);
        guiManager = new GuiManager(this, gameManager);
        teamChatManager = new TeamChatManager(this);
        
        // Initialize listeners
        playerListener = new PlayerListener(this, gameManager, teamChatManager);
        guiListener = new GuiListener(this, gameManager, guiManager);
        lobbyProtectionListener = new LobbyProtectionListener(gameManager);
        gameItemProtectionListener = new GameItemProtectionListener(gameManager);
        headstartListener = new HeadstartListener(this, headstartManager);
        
        // Register event listeners
        getServer().getPluginManager().registerEvents(playerListener, this);
        getServer().getPluginManager().registerEvents(guiListener, this);
        getServer().getPluginManager().registerEvents(lobbyProtectionListener, this);
        getServer().getPluginManager().registerEvents(gameItemProtectionListener, this);
        getServer().getPluginManager().registerEvents(headstartListener, this);
        
        // Register commands
        getCommand("manhunt").setExecutor(new ManhuntCommand(this, gameManager, guiManager));
        getCommand("teamhunters").setExecutor(new TeamHuntersCommand(gameManager, guiManager));
        getCommand("teamrunners").setExecutor(new TeamRunnersCommand(gameManager, guiManager));
        getCommand("quitgame").setExecutor(new QuitGameCommand(gameManager, guiManager));
        getCommand("toall").setExecutor(new ChatToggleCommands.ToAllCommand(playerListener, gameManager));
        getCommand("toteam").setExecutor(new ChatToggleCommands.ToTeamCommand(playerListener, gameManager));
        
        LOGGER.info("Manhunt Deluxe plugin has been enabled!");
    }
    
    @Override
    public void onDisable() {
        // Clean up active games
        if (gameManager != null) {
            gameManager.cleanup();
        }
        
        LOGGER.info("Manhunt Deluxe plugin has been disabled!");
    }
    
    /**
     * Gets the active game manager.
     * 
     * @return The game manager
     */
    public GameManager getGameManager() {
        return gameManager;
    }
    
    /**
     * Gets the GUI manager.
     * 
     * @return The GUI manager
     */
    public GuiManager getGuiManager() {
        return guiManager;
    }
    
    /**
     * Gets the team chat manager.
     * 
     * @return The team chat manager
     */
    public TeamChatManager getTeamChatManager() {
        return teamChatManager;
    }

    /**
     * Gets the world management service.
     * 
     * @return The world management service
     */
    public WorldManagementService getWorldManagementService() {
        return worldManagementService;
    }
    
    /**
     * Gets the lobby service.
     * 
     * @return The lobby service
     */
    public LobbyService getLobbyService() {
        return lobbyService;
    }
    
    /**
     * Gets the game task service.
     * 
     * @return The game task service
     */
    public GameTaskService getGameTaskService() {
        return gameTaskService;
    }
    
    /**
     * Gets the headstart manager.
     * 
     * @return The headstart manager
     */
    public HeadstartManager getHeadstartManager() {
        return headstartManager;
    }
}