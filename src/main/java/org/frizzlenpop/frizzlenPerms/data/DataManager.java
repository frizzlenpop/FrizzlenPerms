package org.frizzlenpop.frizzlenPerms.data;

import org.frizzlenpop.frizzlenPerms.FrizzlenPerms;
import org.frizzlenpop.frizzlenPerms.models.AuditLog;
import org.frizzlenpop.frizzlenPerms.models.PlayerData;
import org.frizzlenpop.frizzlenPerms.models.Rank;
import org.frizzlenpop.frizzlenPerms.utils.LogManager;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.Objects;

/**
 * Manager for data storage and retrieval.
 */
public class DataManager {
    
    private final FrizzlenPerms plugin;
    private volatile StorageProvider storageProvider;
    private final Object storageLock = new Object();
    
    // Cache for player data
    private final Map<UUID, PlayerData> playerDataCache = new ConcurrentHashMap<>();
    // Cache for ranks
    private final Map<String, Rank> rankCache = new ConcurrentHashMap<>();
    
    /**
     * Creates a new DataManager with the specified plugin instance.
     *
     * @param plugin The plugin instance
     */
    public DataManager(FrizzlenPerms plugin) {
        this.plugin = Objects.requireNonNull(plugin, "Plugin cannot be null");
    }
    
    /**
     * Initializes the data manager based on the configuration.
     */
    public void initialize() {
        String storageType = plugin.getConfigManager().getStorageType();
        
        synchronized (storageLock) {
            try {
                switch (storageType.toLowerCase()) {
                    case "mysql":
                        storageProvider = new MySQLStorage(plugin);
                        break;
                    case "flatfile":
                        storageProvider = new FlatFileStorage(plugin);
                        break;
                    case "sqlite":
                    default:
                        storageProvider = new SQLiteStorage(plugin);
                        break;
                }
                
                storageProvider.initialize();
                
                // Load all ranks into cache
                Map<String, Rank> ranks = storageProvider.getAllRanks();
                if (ranks != null) {
                    rankCache.putAll(ranks);
                }
                
                plugin.getLogger().info("Data manager initialized with " + storageType + " storage.");
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to initialize data manager", e);
                // Fallback to SQLite if the configured storage fails
                if (!"sqlite".equalsIgnoreCase(storageType)) {
                    plugin.getLogger().warning("Falling back to SQLite storage");
                    try {
                        storageProvider = new SQLiteStorage(plugin);
                        storageProvider.initialize();
                    } catch (Exception ex) {
                        plugin.getLogger().log(Level.SEVERE, "Failed to initialize SQLite fallback storage", ex);
                    }
                }
            }
        }
    }
    
    /**
     * Saves all data to storage.
     */
    public void saveAll() {
        if (storageProvider != null) {
            try {
                // Save player data cache
                for (PlayerData playerData : playerDataCache.values()) {
                    storageProvider.savePlayerData(playerData);
                }
                
                // Save rank cache
                for (Rank rank : rankCache.values()) {
                    storageProvider.saveRank(rank);
                }
                
                plugin.getLogger().info("All data saved successfully.");
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to save all data", e);
            }
        }
    }
    
    /**
     * Closes all database connections.
     */
    public void closeConnections() {
        if (storageProvider != null) {
            storageProvider.closeConnections();
        }
    }
    
    /**
     * Gets a player's data from storage or cache.
     *
     * @param uuid The UUID of the player
     * @return The player data, or null if not found
     */
    public PlayerData getPlayerData(UUID uuid) {
        if (uuid == null) {
            return null;
        }

        // Check cache first
        PlayerData data = playerDataCache.get(uuid);
        
        // If not in cache, load from storage
        if (data == null && storageProvider != null) {
            try {
                data = storageProvider.getPlayerData(uuid);
                if (data != null) {
                    playerDataCache.put(uuid, data);
                }
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to load player data for " + uuid, e);
            }
        }
        
        return data;
    }
    
    /**
     * Gets a player's data from storage by name.
     *
     * @param name The name of the player
     * @return The player data, or null if not found
     */
    public PlayerData getPlayerDataByName(String name) {
        // Check cache first
        for (PlayerData data : playerDataCache.values()) {
            if (data.getPlayerName().equalsIgnoreCase(name)) {
                return data;
            }
        }
        
        // If not in cache, load from storage
        if (storageProvider != null) {
            PlayerData data = storageProvider.getPlayerDataByName(name);
            if (data != null) {
                playerDataCache.put(data.getUuid(), data);
            }
            return data;
        }
        
        return null;
    }
    
    /**
     * Saves a player's data to storage and updates cache.
     *
     * @param playerData The player data to save
     */
    public void savePlayerData(PlayerData playerData) {
        if (playerData != null) {
            // Update cache
            playerDataCache.put(playerData.getUuid(), playerData);
            
            // Save to storage
            if (storageProvider != null) {
                storageProvider.savePlayerData(playerData);
            }
        }
    }
    
    /**
     * Gets a rank from cache or storage.
     *
     * @param name The name of the rank
     * @return The rank, or null if not found
     */
    public Rank getRank(String name) {
        if (name == null) {
            return null;
        }

        String lowercaseName = name.toLowerCase();
        
        // Check cache first
        Rank rank = rankCache.get(lowercaseName);
        
        // If not in cache, load from storage
        if (rank == null && storageProvider != null) {
            try {
                rank = storageProvider.getRank(name);
                if (rank != null) {
                    rankCache.put(lowercaseName, rank);
                }
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to load rank: " + name, e);
            }
        }
        
        return rank;
    }
    
    /**
     * Gets all ranks from cache or storage.
     *
     * @return A map of rank names to ranks
     */
    public Map<String, Rank> getAllRanks() {
        // If cache is empty, load from storage
        if (rankCache.isEmpty() && storageProvider != null) {
            rankCache.putAll(storageProvider.getAllRanks());
        }
        
        return rankCache;
    }
    
    /**
     * Saves a rank to storage and updates cache.
     *
     * @param rank The rank to save
     */
    public void saveRank(Rank rank) {
        if (rank == null) {
            return;
        }

        String lowercaseName = rank.getName().toLowerCase();
        
        try {
            // Update cache
            rankCache.put(lowercaseName, rank);
            
            // Save to storage
            if (storageProvider != null) {
                storageProvider.saveRank(rank);
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save rank: " + rank.getName(), e);
            // Remove from cache if save failed
            rankCache.remove(lowercaseName);
        }
    }
    
    /**
     * Deletes a rank from storage and cache.
     *
     * @param name The name of the rank to delete
     */
    public void deleteRank(String name) {
        if (name == null) {
            return;
        }

        String lowercaseName = name.toLowerCase();
        
        try {
            // Remove from cache
            rankCache.remove(lowercaseName);
            
            // Delete from storage
            if (storageProvider != null) {
                storageProvider.deleteRank(name);
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to delete rank: " + name, e);
        }
    }
    
    /**
     * Adds an audit log entry to storage.
     *
     * @param auditLog The audit log entry to add
     */
    public void addAuditLog(AuditLog auditLog) {
        if (storageProvider != null) {
            storageProvider.addAuditLog(auditLog);
        }
    }
    
    /**
     * Gets audit logs for a player from storage.
     *
     * @param uuid The UUID of the player
     * @param limit The maximum number of logs to retrieve
     * @return A list of audit log entries
     */
    public List<AuditLog> getAuditLogs(UUID uuid, int limit) {
        if (storageProvider != null) {
            return storageProvider.getAuditLogs(uuid, limit);
        }
        return List.of();
    }
    
    /**
     * Gets all audit logs from storage.
     *
     * @param limit The maximum number of logs to retrieve
     * @return A list of audit log entries
     */
    public List<AuditLog> getAllAuditLogs(int limit) {
        if (storageProvider != null) {
            return storageProvider.getAllAuditLogs(limit);
        }
        return List.of();
    }
    
    /**
     * Gets the default rank from cache or storage.
     *
     * @return The default rank, or null if not found
     */
    public Rank getDefaultRank() {
        // Check cache first
        for (Rank rank : rankCache.values()) {
            if (rank.isDefault()) {
                return rank;
            }
        }
        
        // If not found in cache, check storage
        if (storageProvider != null) {
            Rank defaultRank = storageProvider.getDefaultRank();
            if (defaultRank != null) {
                rankCache.put(defaultRank.getName().toLowerCase(), defaultRank);
            }
            return defaultRank;
        }
        
        return null;
    }
    
    /**
     * Clears all caches.
     */
    public void clearCaches() {
        synchronized (storageLock) {
            playerDataCache.clear();
            rankCache.clear();
        }
    }
    
    /**
     * Gets all player data from storage.
     *
     * @return A list of all player data
     */
    public List<PlayerData> getAllPlayerData() {
        if (storageProvider != null) {
            return storageProvider.getAllPlayerData();
        }
        return List.of();
    }
    
    /**
     * Cleans up old audit logs.
     *
     * @param maxEntries The maximum number of entries to keep
     */
    public void cleanupAuditLogs(int maxEntries) {
        if (storageProvider != null) {
            try {
                storageProvider.cleanupAuditLogs(maxEntries);
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to clean up audit logs", e);
            }
        }
    }
    
    /**
     * Deletes a player's data from storage and cache.
     *
     * @param uuid The UUID of the player
     * @return Whether the operation was successful
     */
    public boolean deletePlayerData(UUID uuid) {
        if (uuid == null) {
            return false;
        }

        try {
            // Remove from cache
            playerDataCache.remove(uuid);
            
            // Delete from storage
            if (storageProvider != null) {
                storageProvider.deletePlayerData(uuid);
            }
            
            return true;
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to delete player data for " + uuid, e);
            return false;
        }
    }
} 