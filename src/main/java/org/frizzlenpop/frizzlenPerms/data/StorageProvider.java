package org.frizzlenpop.frizzlenPerms.data;

import org.frizzlenpop.frizzlenPerms.models.AuditLog;
import org.frizzlenpop.frizzlenPerms.models.PlayerData;
import org.frizzlenpop.frizzlenPerms.models.Rank;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Interface for data storage providers.
 */
public interface StorageProvider {
    
    /**
     * Initializes the storage provider.
     */
    void initialize();
    
    /**
     * Closes all connections.
     */
    void closeConnections();
    
    /**
     * Gets a player's data from storage.
     *
     * @param uuid The UUID of the player
     * @return The player data, or null if not found
     */
    PlayerData getPlayerData(UUID uuid);
    
    /**
     * Gets a player's data from storage by name.
     *
     * @param name The name of the player
     * @return The player data, or null if not found
     */
    PlayerData getPlayerDataByName(String name);
    
    /**
     * Saves a player's data to storage.
     *
     * @param playerData The player data to save
     */
    void savePlayerData(PlayerData playerData);
    
    /**
     * Gets a rank from storage.
     *
     * @param name The name of the rank
     * @return The rank, or null if not found
     */
    Rank getRank(String name);
    
    /**
     * Gets all ranks from storage.
     *
     * @return A map of rank names to ranks
     */
    Map<String, Rank> getAllRanks();
    
    /**
     * Saves a rank to storage.
     *
     * @param rank The rank to save
     */
    void saveRank(Rank rank);
    
    /**
     * Deletes a rank from storage.
     *
     * @param name The name of the rank to delete
     */
    void deleteRank(String name);
    
    /**
     * Adds an audit log entry to storage.
     *
     * @param auditLog The audit log entry to add
     */
    void addAuditLog(AuditLog auditLog);
    
    /**
     * Gets audit logs for a player from storage.
     *
     * @param uuid The UUID of the player
     * @param limit The maximum number of logs to retrieve
     * @return A list of audit log entries
     */
    List<AuditLog> getAuditLogs(UUID uuid, int limit);
    
    /**
     * Gets all audit logs from storage.
     *
     * @param limit The maximum number of logs to retrieve
     * @return A list of audit log entries
     */
    List<AuditLog> getAllAuditLogs(int limit);
    
    /**
     * Gets the default rank from storage.
     *
     * @return The default rank, or null if not found
     */
    Rank getDefaultRank();
    
    /**
     * Gets all player data from storage.
     *
     * @return A list of all player data
     */
    List<PlayerData> getAllPlayerData();

    /**
     * Cleans up old audit logs.
     *
     * @param maxEntries The maximum number of entries to keep
     */
    void cleanupAuditLogs(int maxEntries);

    /**
     * Deletes a player's data from storage.
     *
     * @param uuid The UUID of the player
     */
    void deletePlayerData(UUID uuid);
} 