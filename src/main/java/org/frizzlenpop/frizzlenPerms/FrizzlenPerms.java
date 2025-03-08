package org.frizzlenpop.frizzlenPerms;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.frizzlenpop.frizzlenPerms.audit.AuditManager;
import org.frizzlenpop.frizzlenPerms.commands.CommandManager;
import org.frizzlenpop.frizzlenPerms.data.DataManager;
import org.frizzlenpop.frizzlenPerms.discord.DiscordManager;
import org.frizzlenpop.frizzlenPerms.gui.GuiManager;
import org.frizzlenpop.frizzlenPerms.listeners.PlayerListener;
import org.frizzlenpop.frizzlenPerms.permissions.PermissionManager;
import org.frizzlenpop.frizzlenPerms.ranks.RankManager;
import org.frizzlenpop.frizzlenPerms.sync.SyncManager;
import org.frizzlenpop.frizzlenPerms.utils.MessageUtils;

import java.util.logging.Logger;

public final class FrizzlenPerms extends JavaPlugin {

    private static FrizzlenPerms instance;
    private Logger logger;
    
    // Managers
    private ConfigManager configManager;
    private DataManager dataManager;
    private RankManager rankManager;
    private PermissionManager permissionManager;
    private CommandManager commandManager;
    private GuiManager guiManager;
    private DiscordManager discordManager;
    private SyncManager syncManager;
    private AuditManager auditManager;
    
    @Override
    public void onEnable() {
        // Store instance for static access
        instance = this;
        logger = getLogger();
        
        // Log startup message
        logger.info("§a━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        logger.info("§f                      FrizzlenPerms                         ");
        logger.info("§f                    Version: " + getDescription().getVersion());
        logger.info("§a━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        
        // Initialize managers in correct order
        initializeManagers();
        
        // Register event listeners
        registerListeners();
        
        logger.info("Plugin has been enabled successfully!");
    }

    @Override
    public void onDisable() {
        logger.info("Shutting down FrizzlenPerms...");
        
        // Cleanup resources in order
        if (permissionManager != null) {
            permissionManager.cleanup();
        }
        
        // Disconnect from Discord if connected
        if (discordManager != null) {
            discordManager.shutdown();
        }
        
        // Save all data and close connections
        if (dataManager != null) {
            try {
                dataManager.saveAll();
                dataManager.closeConnections();
            } catch (Exception e) {
                logger.warning("Error while closing data manager: " + e.getMessage());
            }
        }
        
        logger.info("FrizzlenPerms has been disabled.");
        
        // Clear static instance
        instance = null;
    }
    
    private void initializeManagers() {
        // Order matters here for dependency injection
        
        // 1. Config manager (needed by everything else)
        configManager = new ConfigManager(this);
        
        // 2. Initialize MessageUtils
        MessageUtils.initialize(this);
        
        // 3. Data manager (handles storage)
        dataManager = new DataManager(this);
        dataManager.initialize();
        
        // 4. Audit manager (tracks permission/rank changes)
        auditManager = new AuditManager(this, dataManager, configManager);
        
        // 5. Permission manager
        permissionManager = new PermissionManager(this, dataManager);
        permissionManager.initialize();
        
        // 6. Rank manager (depends on the above)
        rankManager = new RankManager(this, dataManager, configManager, permissionManager, auditManager);
        rankManager.initialize();
        
        // 7. Command manager
        commandManager = new CommandManager(this);
        commandManager.registerCommands();
        
        // 8. GUI manager
        guiManager = new GuiManager(this);
        
        // 9. Discord manager (if enabled)
        if (configManager.isDiscordEnabled()) {
            discordManager = new DiscordManager(this);
            discordManager.initialize();
        }
        
        // 10. Sync manager (if enabled)
        if (configManager.isSyncEnabled()) {
            syncManager = new SyncManager(this);
            syncManager.initialize();
        }
    }
    
    private void registerListeners() {
        // Register the player listener
        PlayerListener playerListener = new PlayerListener(this, dataManager, permissionManager, rankManager);
        Bukkit.getPluginManager().registerEvents(playerListener, this);
        
        // Register GUI listeners if GUI is enabled
        if (configManager.isGuiEnabled() && guiManager != null) {
            Bukkit.getPluginManager().registerEvents(guiManager, this);
        }
    }
    
    // Static accessor for plugin instance
    public static FrizzlenPerms getInstance() {
        return instance;
    }
    
    // Getter methods for all managers
    public ConfigManager getConfigManager() {
        return configManager;
    }
    
    public DataManager getDataManager() {
        return dataManager;
    }
    
    public RankManager getRankManager() {
        return rankManager;
    }
    
    public PermissionManager getPermissionManager() {
        return permissionManager;
    }
    
    public CommandManager getCommandManager() {
        return commandManager;
    }
    
    public GuiManager getGuiManager() {
        return guiManager;
    }
    
    public DiscordManager getDiscordManager() {
        return discordManager;
    }
    
    public SyncManager getSyncManager() {
        return syncManager;
    }
    
    public AuditManager getAuditManager() {
        return auditManager;
    }
}
