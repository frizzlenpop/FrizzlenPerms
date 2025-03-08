package org.frizzlenpop.frizzlenPerms.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import org.frizzlenpop.frizzlenPerms.FrizzlenPerms;
import org.frizzlenpop.frizzlenPerms.models.AuditLog;
import org.frizzlenpop.frizzlenPerms.models.PlayerData;
import org.frizzlenpop.frizzlenPerms.models.Rank;

import java.io.*;
import java.lang.reflect.Type;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.stream.Collectors;

/**
 * FlatFile implementation of the StorageProvider interface using JSON files.
 */
public class FlatFileStorage implements StorageProvider {
    
    private final FrizzlenPerms plugin;
    private final Gson gson;
    
    private File playersDir;
    private File ranksFile;
    private File auditLogsFile;
    
    private Map<String, Rank> ranks = new ConcurrentHashMap<>();
    private List<AuditLog> auditLogs = Collections.synchronizedList(new ArrayList<>());
    
    /**
     * Creates a new FlatFileStorage with the specified plugin instance.
     *
     * @param plugin The plugin instance
     */
    public FlatFileStorage(FrizzlenPerms plugin) {
        this.plugin = plugin;
        this.gson = new GsonBuilder()
                .setPrettyPrinting()
                .serializeNulls()
                .create();
    }
    
    @Override
    public void initialize() {
        // Create data directory
        File dataDir = new File(plugin.getDataFolder(), "data");
        if (!dataDir.exists()) {
            dataDir.mkdirs();
        }
        
        // Create players directory
        playersDir = new File(dataDir, "players");
        if (!playersDir.exists()) {
            playersDir.mkdirs();
        }
        
        // Initialize ranks file
        ranksFile = new File(dataDir, "ranks.json");
        if (ranksFile.exists()) {
            loadRanks();
        }
        
        // Initialize audit logs file
        auditLogsFile = new File(dataDir, "audit_logs.json");
        if (auditLogsFile.exists()) {
            loadAuditLogs();
        }
        
        plugin.getLogger().info("FlatFile storage initialized.");
    }
    
    @Override
    public void closeConnections() {
        // Save all data before closing
        saveRanks();
        saveAuditLogs();
    }
    
    @Override
    public PlayerData getPlayerData(UUID uuid) {
        File playerFile = getPlayerFile(uuid);
        if (!playerFile.exists()) {
            return null;
        }
        
        try (FileReader reader = new FileReader(playerFile)) {
            return gson.fromJson(reader, PlayerData.class);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to load player data for " + uuid, e);
            return null;
        }
    }
    
    @Override
    public PlayerData getPlayerDataByName(String name) {
        // Scan player files for matching name
        File[] playerFiles = playersDir.listFiles((dir, fileName) -> fileName.endsWith(".json"));
        if (playerFiles == null) {
            return null;
        }
        
        for (File playerFile : playerFiles) {
            try (FileReader reader = new FileReader(playerFile)) {
                PlayerData playerData = gson.fromJson(reader, PlayerData.class);
                if (playerData != null && playerData.getPlayerName().equalsIgnoreCase(name)) {
                    return playerData;
                }
            } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to read player file: " + playerFile.getName(), e);
            }
        }
        
        return null;
    }
    
    @Override
    public void savePlayerData(PlayerData playerData) {
        if (playerData == null) {
            return;
        }
        
        File playerFile = getPlayerFile(playerData.getUuid());
        try (FileWriter writer = new FileWriter(playerFile)) {
            gson.toJson(playerData, writer);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save player data for " + playerData.getUuid(), e);
        }
    }
    
    /**
     * Gets the file for a player's data.
     *
     * @param uuid The UUID of the player
     * @return The player's data file
     */
    private File getPlayerFile(UUID uuid) {
        return new File(playersDir, uuid.toString() + ".json");
    }
    
    @Override
    public Rank getRank(String name) {
        return ranks.get(name.toLowerCase());
    }
    
    @Override
    public Map<String, Rank> getAllRanks() {
        return new HashMap<>(ranks);
    }
    
    @Override
    public void saveRank(Rank rank) {
        if (rank == null) {
            return;
        }
        
        ranks.put(rank.getName().toLowerCase(), rank);
        saveRanks();
    }
    
    /**
     * Saves all ranks to the ranks file.
     */
    private void saveRanks() {
        try (FileWriter writer = new FileWriter(ranksFile)) {
            gson.toJson(ranks.values(), writer);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save ranks", e);
        }
    }
    
    /**
     * Loads ranks from the ranks file.
     */
    private void loadRanks() {
        try (FileReader reader = new FileReader(ranksFile)) {
            Type listType = new TypeToken<ArrayList<Rank>>() {}.getType();
            List<Rank> rankList = gson.fromJson(reader, listType);
            
            ranks.clear();
            if (rankList != null) {
                for (Rank rank : rankList) {
                    ranks.put(rank.getName().toLowerCase(), rank);
                }
            }
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to load ranks", e);
        }
    }
    
    @Override
    public void deleteRank(String name) {
        ranks.remove(name.toLowerCase());
        saveRanks();
    }
    
    @Override
    public void addAuditLog(AuditLog auditLog) {
        if (auditLog == null) {
            return;
        }
        
        auditLogs.add(auditLog);
        saveAuditLogs();
    }
    
    /**
     * Saves all audit logs to the audit logs file.
     */
    private void saveAuditLogs() {
        try (FileWriter writer = new FileWriter(auditLogsFile)) {
            gson.toJson(auditLogs, writer);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save audit logs", e);
        }
    }
    
    /**
     * Loads audit logs from the audit logs file.
     */
    private void loadAuditLogs() {
        try (FileReader reader = new FileReader(auditLogsFile)) {
            Type listType = new TypeToken<ArrayList<AuditLog>>() {}.getType();
            List<AuditLog> loadedLogs = gson.fromJson(reader, listType);
            
            auditLogs.clear();
            if (loadedLogs != null) {
                auditLogs.addAll(loadedLogs);
            }
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to load audit logs", e);
        }
    }
    
    @Override
    public List<AuditLog> getAuditLogs(UUID uuid, int limit) {
        return auditLogs.stream()
                .filter(log -> uuid.equals(log.getTargetUuid()))
                .sorted(Comparator.comparingLong(AuditLog::getTimestamp).reversed())
                .limit(limit)
                .collect(Collectors.toList());
    }
    
    @Override
    public List<AuditLog> getAllAuditLogs(int limit) {
        return auditLogs.stream()
                .sorted(Comparator.comparingLong(AuditLog::getTimestamp).reversed())
                .limit(limit)
                .collect(Collectors.toList());
    }
    
    @Override
    public Rank getDefaultRank() {
        for (Rank rank : ranks.values()) {
            if (rank.isDefault()) {
                return rank;
            }
        }
        return null;
    }

    @Override
    public void cleanupAuditLogs(int daysToKeep) {
        long cutoffTime = System.currentTimeMillis() - (daysToKeep * 24L * 60L * 60L * 1000L);
        auditLogs.removeIf(log -> log.getTimestamp() < cutoffTime);
        saveAuditLogs();
    }

    @Override
    public void deletePlayerData(UUID uuid) {
        if (uuid == null) {
            return;
        }

        File playerFile = getPlayerFile(uuid);
        if (playerFile.exists()) {
            if (!playerFile.delete()) {
                plugin.getLogger().warning("Failed to delete player data file for " + uuid);
            }
        }
    }

    @Override
    public List<PlayerData> getAllPlayerData() {
        List<PlayerData> allPlayers = new ArrayList<>();
        
        File[] playerFiles = playersDir.listFiles((dir, fileName) -> fileName.endsWith(".json"));
        if (playerFiles == null) {
            return allPlayers;
        }
        
        for (File playerFile : playerFiles) {
            try (FileReader reader = new FileReader(playerFile)) {
                PlayerData playerData = gson.fromJson(reader, PlayerData.class);
                if (playerData != null) {
                    allPlayers.add(playerData);
                }
            } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to read player file: " + playerFile.getName(), e);
            }
        }
        
        return allPlayers;
    }
} 