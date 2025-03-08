package org.frizzlenpop.frizzlenPerms.audit;

import org.frizzlenpop.frizzlenPerms.ConfigManager;
import org.frizzlenpop.frizzlenPerms.FrizzlenPerms;
import org.frizzlenpop.frizzlenPerms.data.DataManager;
import org.frizzlenpop.frizzlenPerms.models.AuditLog;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.Objects;

/**
 * Manages audit logging for plugin actions.
 */
public class AuditManager {
    
    private final FrizzlenPerms plugin;
    private final DataManager dataManager;
    private final ConfigManager configManager;
    private final File logFile;
    private final SimpleDateFormat dateFormat;
    private final Object logLock = new Object();
    
    /**
     * Creates a new AuditManager instance.
     *
     * @param plugin The plugin instance
     * @param dataManager The data manager instance
     * @param configManager The config manager instance
     */
    public AuditManager(FrizzlenPerms plugin, DataManager dataManager, ConfigManager configManager) {
        this.plugin = Objects.requireNonNull(plugin, "Plugin cannot be null");
        this.dataManager = Objects.requireNonNull(dataManager, "DataManager cannot be null");
        this.configManager = Objects.requireNonNull(configManager, "ConfigManager cannot be null");
        this.dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        
        // Set up audit log file
        File logsDir = new File(plugin.getDataFolder(), "logs");
        if (!logsDir.exists() && !logsDir.mkdirs()) {
            plugin.getLogger().warning("Failed to create logs directory");
        }
        
        this.logFile = new File(logsDir, "audit.log");
        try {
            if (!logFile.exists() && !logFile.createNewFile()) {
                plugin.getLogger().warning("Failed to create audit log file");
            }
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "Failed to create audit log file", e);
        }
    }
    
    /**
     * Logs an action to the audit log.
     *
     * @param performerId The UUID of the player who performed the action, or null for console
     * @param performerName The name of the player who performed the action, or "Console"
     * @param actionType The type of action performed
     * @param target The target of the action (rank name, player name, etc.)
     * @param details Additional details about the action
     * @param server The server where the action was performed
     */
    public void logAction(UUID performerId, String performerName, AuditLog.ActionType actionType, 
                         String target, String details, String server) {
        logAction(performerId, performerName, actionType, target, details, server, null);
    }
    
    /**
     * Logs an action to the audit log with a target player ID.
     *
     * @param performerId The UUID of the player who performed the action, or null for console
     * @param performerName The name of the player who performed the action, or "Console"
     * @param actionType The type of action performed
     * @param target The target of the action (rank name, player name, etc.)
     * @param details Additional details about the action
     * @param server The server where the action was performed
     * @param targetPlayerId The UUID of the target player
     */
    public void logAction(UUID performerId, String performerName, AuditLog.ActionType actionType, 
                         String target, String details, String server, UUID targetPlayerId) {
        // Check if logging is enabled
        if (!configManager.isLoggingEnabled()) {
            return;
        }
        
        if (actionType == null) {
            plugin.getLogger().warning("Cannot log action with null action type");
            return;
        }
        
        if (performerName == null) {
            performerName = "Console";
        }
        
        if (target == null) {
            target = "Unknown";
        }
        
        if (details == null) {
            details = "";
        }
        
        if (server == null) {
            server = configManager.getServerName();
        }
        
        try {
            // Create the audit log entry
            AuditLog log = new AuditLog(
                UUID.randomUUID(), // Generate unique ID for the log entry
                performerId,
                performerName,
                System.currentTimeMillis(),
                actionType,
                details,
                server,
                targetPlayerId
            );
            
            // Save to database
            dataManager.addAuditLog(log);
            
            // Log to file if enabled
            if (configManager.isLoggingSavedToFile()) {
                synchronized (logLock) {
                    logToFile(log);
                }
            }
            
            // Clean up old entries if needed
            int maxEntries = configManager.getMaxLogEntries();
            if (maxEntries > 0) {
                cleanupOldEntries(maxEntries);
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to log action", e);
        }
    }
    
    /**
     * Logs an action to the audit log with a target Discord ID.
     *
     * @param performerId The UUID of the player who performed the action, or null for console
     * @param performerName The name of the player who performed the action, or "Console"
     * @param actionType The type of action performed
     * @param target The target of the action (rank name, player name, etc.)
     * @param details Additional details about the action
     * @param server The server where the action was performed
     * @param targetDiscordId The Discord ID of the target
     */
    public void logActionWithDiscordTarget(UUID performerId, String performerName, AuditLog.ActionType actionType, 
                                          String target, String details, String server, String targetDiscordId) {
        // Check if logging is enabled
        if (!configManager.isLoggingEnabled()) {
            return;
        }
        
        // Create the audit log entry
        AuditLog log = new AuditLog(
            UUID.randomUUID(),
            performerId,
            performerName,
            System.currentTimeMillis(),
            actionType,
            details,
            server,
            null,
            targetDiscordId
        );
        
        // Save to database
        dataManager.addAuditLog(log);
        
        // Log to file if enabled
        if (configManager.isLoggingSavedToFile()) {
            logToFile(log);
        }
        
        // Clean up old entries if needed
        int maxEntries = configManager.getMaxLogEntries();
        if (maxEntries > 0) {
            cleanupOldEntries(maxEntries);
        }
    }
    
    /**
     * Logs an audit log entry to the file.
     *
     * @param log The audit log entry to log
     */
    private void logToFile(AuditLog log) {
        if (logFile == null || log == null) {
            return;
        }
        
        synchronized (logLock) {
            try (PrintWriter writer = new PrintWriter(new FileWriter(logFile, true))) {
                String timestamp = dateFormat.format(new Date(log.getTimestamp()));
                writer.println("[" + timestamp + "] " + log.format());
                writer.flush();
            } catch (IOException e) {
                plugin.getLogger().log(Level.WARNING, "Failed to write to audit log file", e);
            }
        }
    }
    
    /**
     * Cleans up old audit log entries.
     *
     * @param maxEntries The maximum number of entries to keep
     */
    private void cleanupOldEntries(int maxEntries) {
        dataManager.cleanupAuditLogs(maxEntries);
    }
    
    /**
     * Gets audit logs for a player.
     *
     * @param uuid The UUID of the player
     * @param limit The maximum number of logs to return
     * @return The list of audit logs for the player
     */
    public List<AuditLog> getAuditLogsForPlayer(UUID uuid, int limit) {
        if (uuid == null) {
            return List.of();
        }
        
        if (limit <= 0) {
            limit = configManager.getMaxLogEntries();
        }
        
        try {
            return dataManager.getAuditLogs(uuid, limit);
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to get audit logs for player " + uuid, e);
            return List.of();
        }
    }
    
    /**
     * Gets all audit logs.
     *
     * @param limit The maximum number of logs to return
     * @return The list of all audit logs
     */
    public List<AuditLog> getAllAuditLogs(int limit) {
        if (limit <= 0) {
            limit = configManager.getMaxLogEntries();
        }
        
        try {
            return dataManager.getAllAuditLogs(limit);
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to get all audit logs", e);
            return List.of();
        }
    }
} 