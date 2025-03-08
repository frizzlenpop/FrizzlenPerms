package org.frizzlenpop.frizzlenPerms.data;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.frizzlenpop.frizzlenPerms.FrizzlenPerms;
import org.frizzlenpop.frizzlenPerms.models.AuditLog;
import org.frizzlenpop.frizzlenPerms.models.PlayerData;
import org.frizzlenpop.frizzlenPerms.models.Rank;
import org.frizzlenpop.frizzlenPerms.utils.LogManager;

import java.sql.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * MySQL implementation of the StorageProvider interface.
 */
public class MySQLStorage implements StorageProvider {

    private final FrizzlenPerms plugin;
    private HikariDataSource dataSource;
    private boolean initialized = false;
    private final String host;
    private final int port;
    private final String database;
    private final String username;
    private final String password;
    private final boolean useSSL;
    private final int poolSize;
    private final Map<UUID, PlayerData> playerCache = new ConcurrentHashMap<>();
    private final Map<String, Rank> rankCache = new ConcurrentHashMap<>();

    public MySQLStorage(FrizzlenPerms plugin) {
        this.plugin = plugin;
        
        // Load MySQL configuration
        this.host = plugin.getConfigManager().getMySQLHost();
        this.port = plugin.getConfigManager().getMySQLPort();
        this.database = plugin.getConfigManager().getMySQLDatabase();
        this.username = plugin.getConfigManager().getMySQLUsername();
        this.password = plugin.getConfigManager().getMySQLPassword();
        this.useSSL = false;
        this.poolSize = plugin.getConfigManager().getMySQLPoolSize();
    }

    @Override
    public void initialize() {
        if (initialized) {
            return;
        }

        try {
            HikariConfig config = new HikariConfig();
            config.setDriverClassName("com.mysql.jdbc.Driver");
            config.setJdbcUrl(String.format("jdbc:mysql://%s:%d/%s?useSSL=%s", host, port, database, useSSL));
            config.setUsername(username);
            config.setPassword(password);
            config.setMaximumPoolSize(poolSize);
            config.setConnectionTimeout(30000);
            config.setPoolName("FrizzlenPerms-MySQL");
            
            dataSource = new HikariDataSource(config);
            createTables();
            
            initialized = true;
            plugin.getLogger().info("MySQL storage initialized.");
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to initialize MySQL storage", e);
            throw new RuntimeException("Failed to initialize MySQL storage", e);
        }
    }

    private void createTables() throws SQLException {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            
            // Create ranks table
            stmt.execute("CREATE TABLE IF NOT EXISTS ranks (" +
                    "name VARCHAR(64) PRIMARY KEY, " +
                    "prefix VARCHAR(64), " +
                    "suffix VARCHAR(64), " +
                    "chat_color VARCHAR(16), " +
                    "parent_rank VARCHAR(64), " +
                    "is_default BOOLEAN, " +
                    "weight INT, " +
                    "permissions TEXT)");
            
            // Create world_permissions table for ranks
            stmt.execute("CREATE TABLE IF NOT EXISTS rank_world_permissions (" +
                    "rank_name VARCHAR(64), " +
                    "world VARCHAR(64), " +
                    "permissions TEXT, " +
                    "PRIMARY KEY (rank_name, world))");
            
            // Create players table
            stmt.execute("CREATE TABLE IF NOT EXISTS players (" +
                    "uuid VARCHAR(36) PRIMARY KEY, " +
                    "player_name VARCHAR(16), " +
                    "primary_rank VARCHAR(64), " +
                    "secondary_ranks TEXT, " +
                    "permissions TEXT, " +
                    "discord_id VARCHAR(20), " +
                    "last_seen BIGINT, " +
                    "last_login BIGINT, " +
                    "metadata TEXT)");
            
            // Create world_permissions table for players
            stmt.execute("CREATE TABLE IF NOT EXISTS player_world_permissions (" +
                    "player_uuid VARCHAR(36), " +
                    "world VARCHAR(64), " +
                    "permissions TEXT, " +
                    "PRIMARY KEY (player_uuid, world))");
            
            // Create temporary_ranks table
            stmt.execute("CREATE TABLE IF NOT EXISTS temporary_ranks (" +
                    "player_uuid VARCHAR(36), " +
                    "rank_name VARCHAR(64), " +
                    "expiration BIGINT, " +
                    "PRIMARY KEY (player_uuid, rank_name))");
            
            // Create temporary_permissions table
            stmt.execute("CREATE TABLE IF NOT EXISTS temporary_permissions (" +
                    "player_uuid VARCHAR(36), " +
                    "permission VARCHAR(255), " +
                    "expiration BIGINT, " +
                    "PRIMARY KEY (player_uuid, permission))");
            
            // Create audit_logs table
            stmt.execute("CREATE TABLE IF NOT EXISTS audit_logs (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY, " +
                    "timestamp BIGINT, " +
                    "type VARCHAR(32), " +
                    "actor_uuid VARCHAR(36), " +
                    "target_uuid VARCHAR(36), " +
                    "action_data TEXT)");
            
            // Create indexes
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_players_name ON players (player_name)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_audit_logs_timestamp ON audit_logs (timestamp)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_audit_logs_target ON audit_logs (target_uuid)");
            
        }
    }

    @Override
    public void closeConnections() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }

    @Override
    public PlayerData getPlayerData(UUID uuid) {
        if (!initialized) {
            initialize();
        }
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT * FROM players WHERE uuid = ?")) {
            
            stmt.setString(1, uuid.toString());
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return loadPlayerDataFromResultSet(rs);
                }
            }
        } catch (SQLException e) {
            LogManager.getLogger().log(Level.SEVERE, "Failed to get player data for " + uuid, e);
        }
        
        return null;
    }

    @Override
    public PlayerData getPlayerDataByName(String name) {
        if (!initialized) {
            initialize();
        }
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT * FROM players WHERE player_name = ?")) {
            
            stmt.setString(1, name);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return loadPlayerDataFromResultSet(rs);
                }
            }
        } catch (SQLException e) {
            LogManager.getLogger().log(Level.SEVERE, "Failed to get player data for " + name, e);
        }
        
        return null;
    }

    private PlayerData loadPlayerDataFromResultSet(ResultSet rs) throws SQLException {
        UUID uuid = UUID.fromString(rs.getString("uuid"));
        String playerName = rs.getString("player_name");
        
        PlayerData playerData = new PlayerData(uuid, playerName);
        
        // Set primary rank
        playerData.setPrimaryRank(rs.getString("primary_rank"));
        
        // Set secondary ranks
        String secondaryRanksStr = rs.getString("secondary_ranks");
        if (secondaryRanksStr != null && !secondaryRanksStr.isEmpty()) {
            playerData.setSecondaryRanks(Arrays.asList(secondaryRanksStr.split(",")));
        }
        
        // Set permissions
        String permissionsStr = rs.getString("permissions");
        if (permissionsStr != null && !permissionsStr.isEmpty()) {
            playerData.setPermissions(new HashSet<>(Arrays.asList(permissionsStr.split(","))));
        }
        
        // Set Discord ID
        playerData.setDiscordId(rs.getString("discord_id"));
        
        // Set last seen and last login
        playerData.setLastSeen(rs.getLong("last_seen"));
        playerData.setLastLogin(rs.getLong("last_login"));
        
        // Set metadata
        String metadataStr = rs.getString("metadata");
        if (metadataStr != null && !metadataStr.isEmpty()) {
            Map<String, String> metadata = new HashMap<>();
            for (String entry : metadataStr.split(",")) {
                String[] keyValue = entry.split("=", 2);
                if (keyValue.length == 2) {
                    metadata.put(keyValue[0], keyValue[1]);
                }
            }
            for (Map.Entry<String, String> entry : metadata.entrySet()) {
                playerData.setMetadata(entry.getKey(), entry.getValue());
            }
        }
        
        // Load world permissions
        loadWorldPermissions(playerData);
        
        // Load temporary ranks
        loadTemporaryRanks(playerData);
        
        // Load temporary permissions
        loadTemporaryPermissions(playerData);
        
        return playerData;
    }

    private void loadWorldPermissions(PlayerData playerData) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT * FROM player_world_permissions WHERE player_uuid = ?")) {
            
            stmt.setString(1, playerData.getUuid().toString());
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    String world = rs.getString("world");
                    String permissionsStr = rs.getString("permissions");
                    
                    if (permissionsStr != null && !permissionsStr.isEmpty()) {
                        for (String permission : permissionsStr.split(",")) {
                            playerData.addWorldPermission(world, permission);
                        }
                    }
                }
            }
        } catch (SQLException e) {
            LogManager.getLogger().log(Level.SEVERE, "Failed to load world permissions for " + playerData.getUuid(), e);
        }
    }

    private void loadTemporaryRanks(PlayerData playerData) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT * FROM temporary_ranks WHERE player_uuid = ?")) {
            
            stmt.setString(1, playerData.getUuid().toString());
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    String rankName = rs.getString("rank_name");
                    long expiration = rs.getLong("expiration");
                    
                    playerData.addTemporaryRank(rankName, expiration);
                }
            }
        } catch (SQLException e) {
            LogManager.getLogger().log(Level.SEVERE, "Failed to load temporary ranks for " + playerData.getUuid(), e);
        }
    }

    private void loadTemporaryPermissions(PlayerData playerData) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT * FROM temporary_permissions WHERE player_uuid = ?")) {
            
            stmt.setString(1, playerData.getUuid().toString());
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    String permission = rs.getString("permission");
                    long expiration = rs.getLong("expiration");
                    
                    playerData.addTemporaryPermission(permission, expiration);
                }
            }
        } catch (SQLException e) {
            LogManager.getLogger().log(Level.SEVERE, "Failed to load temporary permissions for " + playerData.getUuid(), e);
        }
    }

    @Override
    public void savePlayerData(PlayerData playerData) {
        if (!initialized) {
            initialize();
        }
        
        try (Connection conn = dataSource.getConnection()) {
            // Start transaction
            conn.setAutoCommit(false);
            
            try {
                // Save player data
                savePlayerDataBase(conn, playerData);
                
                // Save world permissions
                savePlayerWorldPermissions(conn, playerData);
                
                // Save temporary ranks
                saveTemporaryRanks(conn, playerData);
                
                // Save temporary permissions
                saveTemporaryPermissions(conn, playerData);
                
                // Commit transaction
                conn.commit();
            } catch (SQLException e) {
                // Rollback transaction
                conn.rollback();
                throw e;
            } finally {
                // Restore auto-commit
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            LogManager.getLogger().log(Level.SEVERE, "Failed to save player data for " + playerData.getUuid(), e);
        }
    }

    private void savePlayerDataBase(Connection conn, PlayerData playerData) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement(
                "INSERT INTO players (uuid, player_name, primary_rank, secondary_ranks, permissions, discord_id, last_seen, last_login, metadata) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?) " +
                        "ON DUPLICATE KEY UPDATE player_name = ?, primary_rank = ?, secondary_ranks = ?, permissions = ?, " +
                        "discord_id = ?, last_seen = ?, last_login = ?, metadata = ?")) {
            
            // Parameters for INSERT
            stmt.setString(1, playerData.getUuid().toString());
            stmt.setString(2, playerData.getPlayerName());
            stmt.setString(3, playerData.getPrimaryRank());
            
            // Convert secondary ranks to string
            String secondaryRanks = String.join(",", playerData.getSecondaryRanks());
            stmt.setString(4, secondaryRanks);
            
            // Convert permissions to string
            String permissions = String.join(",", playerData.getPermissions());
            stmt.setString(5, permissions);
            
            stmt.setString(6, playerData.getDiscordId());
            stmt.setLong(7, playerData.getLastSeen());
            stmt.setLong(8, playerData.getLastLogin());
            
            // Convert metadata to string
            StringBuilder metadataBuilder = new StringBuilder();
            for (Map.Entry<String, String> entry : playerData.getMetadata().entrySet()) {
                if (metadataBuilder.length() > 0) {
                    metadataBuilder.append(",");
                }
                metadataBuilder.append(entry.getKey()).append("=").append(entry.getValue());
            }
            stmt.setString(9, metadataBuilder.toString());
            
            // Parameters for UPDATE
            stmt.setString(10, playerData.getPlayerName());
            stmt.setString(11, playerData.getPrimaryRank());
            stmt.setString(12, secondaryRanks);
            stmt.setString(13, permissions);
            stmt.setString(14, playerData.getDiscordId());
            stmt.setLong(15, playerData.getLastSeen());
            stmt.setLong(16, playerData.getLastLogin());
            stmt.setString(17, metadataBuilder.toString());
            
            stmt.executeUpdate();
        }
    }

    private void savePlayerWorldPermissions(Connection conn, PlayerData playerData) throws SQLException {
        // Delete existing world permissions
        try (PreparedStatement stmt = conn.prepareStatement(
                "DELETE FROM player_world_permissions WHERE player_uuid = ?")) {
            
            stmt.setString(1, playerData.getUuid().toString());
            stmt.executeUpdate();
        }
        
        // Insert new world permissions
        try (PreparedStatement stmt = conn.prepareStatement(
                "INSERT INTO player_world_permissions (player_uuid, world, permissions) VALUES (?, ?, ?)")) {
            
            for (Map.Entry<String, Set<String>> entry : playerData.getWorldPermissions().entrySet()) {
                String world = entry.getKey();
                Set<String> permissions = entry.getValue();
                
                if (!permissions.isEmpty()) {
                    stmt.setString(1, playerData.getUuid().toString());
                    stmt.setString(2, world);
                    stmt.setString(3, String.join(",", permissions));
                    stmt.executeUpdate();
                }
            }
        }
    }

    private void saveTemporaryRanks(Connection conn, PlayerData playerData) throws SQLException {
        // Delete existing temporary ranks
        try (PreparedStatement stmt = conn.prepareStatement(
                "DELETE FROM temporary_ranks WHERE player_uuid = ?")) {
            
            stmt.setString(1, playerData.getUuid().toString());
            stmt.executeUpdate();
        }
        
        // Insert new temporary ranks
        try (PreparedStatement stmt = conn.prepareStatement(
                "INSERT INTO temporary_ranks (player_uuid, rank_name, expiration) VALUES (?, ?, ?)")) {
            
            for (Map.Entry<String, Long> entry : playerData.getTemporaryRanks().entrySet()) {
                stmt.setString(1, playerData.getUuid().toString());
                stmt.setString(2, entry.getKey());
                stmt.setLong(3, entry.getValue());
                stmt.executeUpdate();
            }
        }
    }

    private void saveTemporaryPermissions(Connection conn, PlayerData playerData) throws SQLException {
        // Delete existing temporary permissions
        try (PreparedStatement stmt = conn.prepareStatement(
                "DELETE FROM temporary_permissions WHERE player_uuid = ?")) {
            
            stmt.setString(1, playerData.getUuid().toString());
            stmt.executeUpdate();
        }
        
        // Insert new temporary permissions
        try (PreparedStatement stmt = conn.prepareStatement(
                "INSERT INTO temporary_permissions (player_uuid, permission, expiration) VALUES (?, ?, ?)")) {
            
            for (Map.Entry<String, Long> entry : playerData.getTemporaryPermissions().entrySet()) {
                stmt.setString(1, playerData.getUuid().toString());
                stmt.setString(2, entry.getKey());
                stmt.setLong(3, entry.getValue());
                stmt.executeUpdate();
            }
        }
    }

    @Override
    public Rank getRank(String name) {
        if (!initialized) {
            initialize();
        }
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT * FROM ranks WHERE name = ?")) {
            
            stmt.setString(1, name);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return loadRankFromResultSet(rs);
                }
            }
        } catch (SQLException e) {
            LogManager.getLogger().log(Level.SEVERE, "Failed to get rank " + name, e);
        }
        
        return null;
    }

    @Override
    public Map<String, Rank> getAllRanks() {
        Map<String, Rank> ranks = new HashMap<>();
        
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM ranks")) {
            
            while (rs.next()) {
                Rank rank = loadRankFromResultSet(rs);
                ranks.put(rank.getName(), rank);
            }
            
        } catch (SQLException e) {
            LogManager.getLogger().log(Level.SEVERE, "Failed to get all ranks", e);
        }
        
        return ranks;
    }

    private Rank loadRankFromResultSet(ResultSet rs) throws SQLException {
        String name = rs.getString("name");
        String displayName = rs.getString("display_name");
        String prefix = rs.getString("prefix");
        String suffix = rs.getString("suffix");
        String chatColor = rs.getString("chat_color");
        String parentRank = rs.getString("parent_rank");
        boolean isDefault = rs.getInt("is_default") == 1;
        int weight = rs.getInt("weight");
        
        Rank rank = new Rank(name, displayName, prefix, suffix, chatColor, weight, parentRank);
        rank.setDefault(isDefault);
        
        // Set permissions
        String permissionsStr = rs.getString("permissions");
        if (permissionsStr != null && !permissionsStr.isEmpty()) {
            for (String permission : permissionsStr.split(",")) {
                rank.addPermission(permission);
            }
        }
        
        // Load world permissions
        loadRankWorldPermissions(rank);
        
        return rank;
    }

    private void loadRankWorldPermissions(Rank rank) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT * FROM rank_world_permissions WHERE rank_name = ?")) {
            
            stmt.setString(1, rank.getName());
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    String world = rs.getString("world");
                    String permissionsStr = rs.getString("permissions");
                    
                    if (permissionsStr != null && !permissionsStr.isEmpty()) {
                        for (String permission : permissionsStr.split(",")) {
                            rank.addWorldPermission(world, permission);
                        }
                    }
                }
            }
        } catch (SQLException e) {
            LogManager.getLogger().log(Level.SEVERE, "Failed to load world permissions for rank " + rank.getName(), e);
        }
    }

    @Override
    public void saveRank(Rank rank) {
        if (!initialized) {
            initialize();
        }
        
        try (Connection conn = dataSource.getConnection()) {
            // Start transaction
            conn.setAutoCommit(false);
            
            try {
                // Save rank data
                saveRankBase(conn, rank);
                
                // Save world permissions
                saveRankWorldPermissions(conn, rank);
                
                // Commit transaction
                conn.commit();
            } catch (SQLException e) {
                // Rollback transaction
                conn.rollback();
                throw e;
            } finally {
                // Restore auto-commit
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            LogManager.getLogger().log(Level.SEVERE, "Failed to save rank " + rank.getName(), e);
        }
    }

    private void saveRankBase(Connection conn, Rank rank) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement(
                "INSERT INTO ranks (name, prefix, suffix, chat_color, parent_rank, is_default, weight, permissions) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?) " +
                        "ON DUPLICATE KEY UPDATE prefix = ?, suffix = ?, chat_color = ?, parent_rank = ?, " +
                        "is_default = ?, weight = ?, permissions = ?")) {
            
            // Parameters for INSERT
            stmt.setString(1, rank.getName());
            stmt.setString(2, rank.getPrefix());
            stmt.setString(3, rank.getSuffix());
            stmt.setString(4, rank.getChatColor());
            stmt.setString(5, rank.getParentRank());
            stmt.setBoolean(6, rank.isDefault());
            stmt.setInt(7, rank.getWeight());
            
            // Convert permissions to string
            String permissions = String.join(",", rank.getPermissions());
            stmt.setString(8, permissions);
            
            // Parameters for UPDATE
            stmt.setString(9, rank.getPrefix());
            stmt.setString(10, rank.getSuffix());
            stmt.setString(11, rank.getChatColor());
            stmt.setString(12, rank.getParentRank());
            stmt.setBoolean(13, rank.isDefault());
            stmt.setInt(14, rank.getWeight());
            stmt.setString(15, permissions);
            
            stmt.executeUpdate();
        }
    }

    private void saveRankWorldPermissions(Connection conn, Rank rank) throws SQLException {
        // Delete existing world permissions
        try (PreparedStatement stmt = conn.prepareStatement(
                "DELETE FROM rank_world_permissions WHERE rank_name = ?")) {
            
            stmt.setString(1, rank.getName());
            stmt.executeUpdate();
        }
        
        // Insert new world permissions
        try (PreparedStatement stmt = conn.prepareStatement(
                "INSERT INTO rank_world_permissions (rank_name, world, permissions) VALUES (?, ?, ?)")) {
            
            for (Map.Entry<String, Set<String>> entry : rank.getWorldPermissions().entrySet()) {
                String world = entry.getKey();
                Set<String> permissions = entry.getValue();
                
                if (!permissions.isEmpty()) {
                    stmt.setString(1, rank.getName());
                    stmt.setString(2, world);
                    stmt.setString(3, String.join(",", permissions));
                    stmt.executeUpdate();
                }
            }
        }
    }

    @Override
    public void deleteRank(String name) {
        if (!initialized) {
            initialize();
        }
        
        try (Connection conn = dataSource.getConnection()) {
            // Start transaction
            conn.setAutoCommit(false);
            
            try {
                // Delete rank world permissions
                try (PreparedStatement stmt = conn.prepareStatement(
                        "DELETE FROM rank_world_permissions WHERE rank_name = ?")) {
                    stmt.setString(1, name);
                    stmt.executeUpdate();
                }
                
                // Delete rank
                try (PreparedStatement stmt = conn.prepareStatement(
                        "DELETE FROM ranks WHERE name = ?")) {
                    stmt.setString(1, name);
                    stmt.executeUpdate();
                }
                
                // Commit transaction
                conn.commit();
            } catch (SQLException e) {
                // Rollback transaction
                conn.rollback();
                throw e;
            } finally {
                // Restore auto-commit
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            LogManager.getLogger().log(Level.SEVERE, "Failed to delete rank " + name, e);
        }
    }

    @Override
    public void addAuditLog(AuditLog auditLog) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                 "INSERT INTO audit_logs (id, timestamp, type, actor_uuid, actor_name, target_uuid, action_data, server) " +
                 "VALUES (?, ?, ?, ?, ?, ?, ?, ?)")) {
            
            stmt.setString(1, auditLog.getId().toString());
            stmt.setLong(2, auditLog.getTimestamp());
            stmt.setString(3, auditLog.getType().getDisplayName());
            stmt.setString(4, auditLog.getActorUuid() != null ? auditLog.getActorUuid().toString() : null);
            stmt.setString(5, auditLog.getActorName());
            stmt.setString(6, auditLog.getTargetUuid() != null ? auditLog.getTargetUuid().toString() : null);
            stmt.setString(7, auditLog.getActionData());
            stmt.setString(8, auditLog.getServer());
            
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to add audit log", e);
        }
    }

    @Override
    public List<AuditLog> getAuditLogs(UUID uuid, int limit) {
        if (!initialized) {
            initialize();
        }
        
        List<AuditLog> logs = new ArrayList<>();
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT * FROM audit_logs WHERE target_uuid = ? ORDER BY timestamp DESC LIMIT ?")) {
            
            stmt.setString(1, uuid.toString());
            stmt.setInt(2, limit);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    logs.add(loadAuditLogFromResultSet(rs));
                }
            }
        } catch (SQLException e) {
            LogManager.getLogger().log(Level.SEVERE, "Failed to get audit logs for " + uuid, e);
        }
        
        return logs;
    }

    @Override
    public List<AuditLog> getAllAuditLogs(int limit) {
        if (!initialized) {
            initialize();
        }
        
        List<AuditLog> logs = new ArrayList<>();
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT * FROM audit_logs ORDER BY timestamp DESC LIMIT ?")) {
            
            stmt.setInt(1, limit);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    logs.add(loadAuditLogFromResultSet(rs));
                }
            }
        } catch (SQLException e) {
            LogManager.getLogger().log(Level.SEVERE, "Failed to get all audit logs", e);
        }
        
        return logs;
    }

    private AuditLog loadAuditLogFromResultSet(ResultSet rs) throws SQLException {
        long timestamp = rs.getLong("timestamp");
        String type = rs.getString("type");
        UUID actorUuid = rs.getString("actor_uuid") != null ? UUID.fromString(rs.getString("actor_uuid")) : null;
        String actorName = rs.getString("actor_name");
        UUID targetUuid = rs.getString("target_uuid") != null ? UUID.fromString(rs.getString("target_uuid")) : null;
        String actionData = rs.getString("action_data");
        String server = rs.getString("server");
        
        return new AuditLog(
            UUID.randomUUID(),
            actorUuid,
            actorName,
            timestamp,
            AuditLog.ActionType.fromDisplayName(type),
            actionData,
            server,
            targetUuid
        );
    }

    @Override
    public Rank getDefaultRank() {
        if (!initialized) {
            initialize();
        }
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT * FROM ranks WHERE is_default = 1")) {
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return loadRankFromResultSet(rs);
                }
            }
        } catch (SQLException e) {
            LogManager.getLogger().log(Level.SEVERE, "Failed to get default rank", e);
        }
        
        return null;
    }

    @Override
    public void cleanupAuditLogs(int maxEntries) {
        if (maxEntries <= 0) {
            return;
        }

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                 "DELETE FROM audit_logs WHERE id NOT IN (" +
                 "SELECT id FROM audit_logs ORDER BY timestamp DESC LIMIT ?)")) {
            
            stmt.setInt(1, maxEntries);
            stmt.executeUpdate();
            
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to clean up audit logs", e);
        }
    }

    @Override
    public List<PlayerData> getAllPlayerData() {
        List<PlayerData> allPlayers = new ArrayList<>();
        
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM players")) {
            
            while (rs.next()) {
                PlayerData playerData = loadPlayerDataFromResultSet(rs);
                if (playerData != null) {
                    allPlayers.add(playerData);
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to get all player data", e);
        }
        
        return allPlayers;
    }
} 