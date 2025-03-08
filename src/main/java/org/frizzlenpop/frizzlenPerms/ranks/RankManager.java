package org.frizzlenpop.frizzlenPerms.ranks;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.frizzlenpop.frizzlenPerms.ConfigManager;
import org.frizzlenpop.frizzlenPerms.FrizzlenPerms;
import org.frizzlenpop.frizzlenPerms.audit.AuditManager;
import org.frizzlenpop.frizzlenPerms.data.DataManager;
import org.frizzlenpop.frizzlenPerms.models.AuditLog;
import org.frizzlenpop.frizzlenPerms.models.PlayerData;
import org.frizzlenpop.frizzlenPerms.models.Rank;
import org.frizzlenpop.frizzlenPerms.permissions.PermissionManager;
import org.frizzlenpop.frizzlenPerms.utils.TimeUtils;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Manages ranks and player-rank assignments.
 */
public class RankManager {
    
    private final FrizzlenPerms plugin;
    private final DataManager dataManager;
    private final ConfigManager configManager;
    private final PermissionManager permissionManager;
    private final AuditManager auditManager;
    private final Map<String, Rank> rankCache;
    private volatile String defaultRankName;
    private final Object rankLock = new Object();
    
    /**
     * Creates a new RankManager instance.
     *
     * @param plugin The plugin instance
     * @param dataManager The data manager instance
     * @param configManager The config manager instance
     * @param permissionManager The permission manager instance
     * @param auditManager The audit manager instance
     */
    public RankManager(FrizzlenPerms plugin, DataManager dataManager, ConfigManager configManager, 
                        PermissionManager permissionManager, AuditManager auditManager) {
        this.plugin = Objects.requireNonNull(plugin, "Plugin cannot be null");
        this.dataManager = Objects.requireNonNull(dataManager, "DataManager cannot be null");
        this.configManager = Objects.requireNonNull(configManager, "ConfigManager cannot be null");
        this.permissionManager = Objects.requireNonNull(permissionManager, "PermissionManager cannot be null");
        this.auditManager = Objects.requireNonNull(auditManager, "AuditManager cannot be null");
        this.rankCache = new ConcurrentHashMap<>();
    }
    
    /**
     * Initializes the rank manager.
     */
    public void initialize() {
        plugin.getLogger().info("Initializing rank manager...");
        synchronized (rankLock) {
            loadDefaultRank();
            createDefaultRanks();
        }
    }
    
    /**
     * Loads the default rank from the config.
     */
    private void loadDefaultRank() {
        defaultRankName = configManager.getString("ranks.default", "default");
        Rank defaultRank = dataManager.getRank(defaultRankName);
        
        if (defaultRank != null) {
            defaultRank.setDefault(true);
            dataManager.saveRank(defaultRank);
            plugin.getLogger().info("Default rank set to: " + defaultRankName);
        } else {
            plugin.getLogger().warning("Default rank not found: " + defaultRankName);
        }
    }
    
    /**
     * Creates default ranks if they don't exist.
     */
    private void createDefaultRanks() {
        // Create default rank if it doesn't exist
        String defaultRankName = configManager.getDefaultRankName();
        createRankIfNotExists(defaultRankName, "Default", "§7[Default] ", 0);
        
        // Set default rank
        Rank defaultRank = getRank(defaultRankName);
        if (defaultRank != null) {
            defaultRank.setDefault(true);
            dataManager.saveRank(defaultRank);
            this.defaultRankName = defaultRankName;
        }
        
        // Create admin rank if it doesn't exist
        String adminRankName = configManager.getAdminRankName();
        createRankIfNotExists(adminRankName, "Admin", "§c[Admin] ", 100);
        
        // Add default permissions to admin rank
        Rank adminRank = getRank(adminRankName);
        if (adminRank != null) {
            List<String> adminPerms = configManager.getAdminRankPermissions();
            for (String perm : adminPerms) {
                adminRank.addPermission(perm);
            }
            dataManager.saveRank(adminRank);
        }
    }
    
    /**
     * Creates a rank if it doesn't exist.
     *
     * @param name The name of the rank
     * @param displayName The display name of the rank
     * @param prefix The prefix of the rank
     * @param weight The weight of the rank
     */
    private void createRankIfNotExists(String name, String displayName, String prefix, int weight) {
        if (getRank(name) == null) {
            Rank rank = new Rank(name, displayName, prefix, "", "§f", weight, null);
            dataManager.saveRank(rank);
            rankCache.put(name.toLowerCase(), rank);
            plugin.getLogger().info("Created rank: " + name);
        }
    }
    
    /**
     * Sets a player's primary rank.
     *
     * @param player The player to set the rank for
     * @param rankName The name of the rank to set
     * @param actor The player setting the rank, or null for console
     * @return Whether the operation was successful
     */
    public boolean setPlayerRank(Player player, String rankName, Player actor) {
        if (player == null || rankName == null) {
            return false;
        }

        UUID uuid = player.getUniqueId();
        PlayerData playerData = dataManager.getPlayerData(uuid);
        
        if (playerData == null) {
            plugin.getLogger().warning("Failed to retrieve player data for " + player.getName() + " (" + uuid + ")");
            return false;
        }
        
        Rank rank = getRank(rankName);
        if (rank == null) {
            plugin.getLogger().warning("Rank not found: " + rankName);
            return false;
        }

        synchronized (rankLock) {
            String oldRank = playerData.getPrimaryRank();
            playerData.setPrimaryRank(rankName);
            dataManager.savePlayerData(playerData);
            
            // Update permissions
            if (player.isOnline()) {
                permissionManager.calculateAndApplyPermissions(player);
            }
            
            // Log the action
            auditManager.logAction(
                actor != null ? actor.getUniqueId() : null,
                actor != null ? actor.getName() : "Console",
                AuditLog.ActionType.RANK_SET,
                playerData.getPlayerName(),
                "Set primary rank to " + rankName + (oldRank != null ? " (was " + oldRank + ")" : ""),
                configManager.getServerName(),
                uuid
            );
        }
        
        return true;
    }
    
    /**
     * Adds a secondary rank to a player.
     *
     * @param player The player to add the rank to
     * @param rankName The name of the rank to add
     * @param actor The player adding the rank, or null for console
     * @return Whether the operation was successful
     */
    public boolean addPlayerSecondaryRank(Player player, String rankName, Player actor) {
        UUID uuid = player.getUniqueId();
        PlayerData playerData = dataManager.getPlayerData(uuid);
        
        if (playerData == null) {
            plugin.getLogger().warning("Failed to retrieve player data for " + player.getName() + " (" + uuid + ")");
            return false;
        }
        
        Rank rank = dataManager.getRank(rankName);
        if (rank == null) {
            plugin.getLogger().warning("Rank not found: " + rankName);
            return false;
        }
        
        // Check anti-abuse limits
        if (configManager.isAntiAbuseEnabled() && 
            playerData.getSecondaryRanks().size() >= configManager.getMaxRanksPerPlayer()) {
            plugin.getLogger().warning("Cannot add more ranks to " + player.getName() + 
                                       ". Maximum limit reached (" + configManager.getMaxRanksPerPlayer() + ")");
            return false;
        }
        
        // Add the rank
        playerData.addSecondaryRank(rankName);
        dataManager.savePlayerData(playerData);
        
        // Update permissions
        if (player.isOnline()) {
            permissionManager.calculateAndApplyPermissions(player);
        }
        
        // Log the action
        auditManager.logAction(
            actor != null ? actor.getUniqueId() : null,
            actor != null ? actor.getName() : "Console",
            AuditLog.ActionType.PLAYER_RANK_ADD,
            player.getName(),
            "Added secondary rank " + rankName,
            configManager.getServerName(),
            uuid
        );
        
        return true;
    }
    
    /**
     * Removes a secondary rank from a player.
     *
     * @param player The player to remove the rank from
     * @param rankName The name of the rank to remove
     * @param actor The player removing the rank, or null for console
     * @return Whether the operation was successful
     */
    public boolean removePlayerSecondaryRank(Player player, String rankName, Player actor) {
        UUID uuid = player.getUniqueId();
        PlayerData playerData = dataManager.getPlayerData(uuid);
        
        if (playerData == null) {
            plugin.getLogger().warning("Failed to retrieve player data for " + player.getName() + " (" + uuid + ")");
            return false;
        }
        
        // Check if the player has the rank
        if (!playerData.getSecondaryRanks().contains(rankName)) {
            plugin.getLogger().warning(player.getName() + " does not have secondary rank: " + rankName);
            return false;
        }
        
        // Remove the rank
        playerData.removeSecondaryRank(rankName);
        dataManager.savePlayerData(playerData);
        
        // Update permissions
        if (player.isOnline()) {
            permissionManager.calculateAndApplyPermissions(player);
        }
        
        // Log the action
        auditManager.logAction(
            actor != null ? actor.getUniqueId() : null,
            actor != null ? actor.getName() : "Console",
            AuditLog.ActionType.PLAYER_RANK_REMOVE,
            player.getName(),
            "Removed secondary rank " + rankName,
            configManager.getServerName(),
            uuid
        );
        
        return true;
    }
    
    /**
     * Adds a temporary rank to a player.
     *
     * @param player The player to add the rank to
     * @param rankName The name of the rank to add
     * @param duration The duration in milliseconds
     * @param asPrimary Whether to set as primary rank
     * @param actor The player adding the rank, or null for console
     * @return Whether the operation was successful
     */
    public boolean addPlayerTemporaryRank(Player player, String rankName, long duration, boolean asPrimary, Player actor) {
        UUID uuid = player.getUniqueId();
        PlayerData playerData = dataManager.getPlayerData(uuid);
        
        if (playerData == null) {
            plugin.getLogger().warning("Failed to retrieve player data for " + player.getName() + " (" + uuid + ")");
            return false;
        }
        
        Rank rank = dataManager.getRank(rankName);
        if (rank == null) {
            plugin.getLogger().warning("Rank not found: " + rankName);
            return false;
        }
        
        // Calculate expiration time
        long expirationTime = System.currentTimeMillis() + duration;
        
        // Check anti-abuse limits for secondary ranks
        if (!asPrimary && configManager.isAntiAbuseEnabled() && 
            playerData.getSecondaryRanks().size() >= configManager.getMaxRanksPerPlayer()) {
            plugin.getLogger().warning("Cannot add more ranks to " + player.getName() + 
                                       ". Maximum limit reached (" + configManager.getMaxRanksPerPlayer() + ")");
            return false;
        }
        
        // Add the rank
        if (asPrimary) {
            playerData.setPrimaryRank(rankName);
        } else {
            playerData.addSecondaryRank(rankName);
        }
        
        // Add to temporary ranks
        playerData.addTemporaryRank(rankName, expirationTime);
        dataManager.savePlayerData(playerData);
        
        // Update permissions
        if (player.isOnline()) {
            permissionManager.calculateAndApplyPermissions(player);
        }
        
        // Log the action
        auditManager.logAction(
            actor != null ? actor.getUniqueId() : null,
            actor != null ? actor.getName() : "Console",
            AuditLog.ActionType.PLAYER_TEMP_RANK_ADD,
            player.getName(),
            "Added temporary " + (asPrimary ? "primary" : "secondary") + " rank " + rankName + 
            " for " + TimeUtils.formatDuration(duration),
            configManager.getServerName(),
            uuid
        );
        
        return true;
    }
    
    /**
     * Removes a temporary rank from a player.
     *
     * @param player The player to remove the rank from
     * @param rankName The name of the rank to remove
     * @param actor The player removing the rank, or null for console
     * @return Whether the operation was successful
     */
    public boolean removePlayerTemporaryRank(Player player, String rankName, Player actor) {
        UUID uuid = player.getUniqueId();
        PlayerData playerData = dataManager.getPlayerData(uuid);
        
        if (playerData == null) {
            plugin.getLogger().warning("Failed to retrieve player data for " + player.getName() + " (" + uuid + ")");
            return false;
        }
        
        // Check if the rank is temporary
        if (!playerData.isTemporaryRank(rankName)) {
            plugin.getLogger().warning(player.getName() + " does not have temporary rank: " + rankName);
            return false;
        }
        
        // Remove the temporary rank
        playerData.removeTemporaryRank(rankName);
        
        // Remove from primary or secondary ranks
        if (rankName.equals(playerData.getPrimaryRank())) {
            playerData.setPrimaryRank(defaultRankName);
        } else {
            playerData.removeSecondaryRank(rankName);
        }
        
        dataManager.savePlayerData(playerData);
        
        // Update permissions
        if (player.isOnline()) {
            permissionManager.calculateAndApplyPermissions(player);
        }
        
        // Log the action
        auditManager.logAction(
            actor != null ? actor.getUniqueId() : null,
            actor != null ? actor.getName() : "Console",
            AuditLog.ActionType.PLAYER_TEMP_RANK_REMOVE,
            player.getName(),
            "Removed temporary rank " + rankName,
            configManager.getServerName(),
            uuid
        );
        
        return true;
    }
    
    /**
     * Creates a new rank.
     *
     * @param name The name of the rank
     * @param displayName The display name of the rank
     * @param prefix The prefix of the rank
     * @param weight The weight of the rank
     * @param actor The player creating the rank
     * @return The created rank
     */
    public Rank createRank(String name, String displayName, String prefix, int weight, Player actor) {
        synchronized (rankLock) {
            // Check if rank already exists
            if (getRank(name) != null) {
                return null;
            }

            // Create new rank
            Rank rank = new Rank(name, displayName, prefix, "", "§f", weight, null);
            dataManager.saveRank(rank);
            rankCache.put(name.toLowerCase(), rank);

            // Log action
            auditManager.logAction(
                actor != null ? actor.getUniqueId() : null,
                actor != null ? actor.getName() : "Console",
                AuditLog.ActionType.RANK_CREATE,
                name,
                "Created rank " + name + " with weight " + weight,
                configManager.getServerName()
            );

            return rank;
        }
    }
    
    /**
     * Creates a rank from a rank object.
     *
     * @param rank The rank object
     * @return True if successful
     */
    public boolean createRankFromObject(Rank rank) {
        if (rank == null) {
            return false;
        }

        synchronized (rankLock) {
            // Check if rank already exists
            if (getRank(rank.getName()) != null) {
                return false;
            }

            // Save rank
            dataManager.saveRank(rank);
            rankCache.put(rank.getName().toLowerCase(), rank);

            return true;
        }
    }
    
    /**
     * Deletes a rank.
     *
     * @param name The name of the rank
     * @param actor The player deleting the rank
     * @return True if successful
     */
    public boolean deleteRank(String name, Player actor) {
        synchronized (rankLock) {
            // Check if rank exists
            Rank rank = getRank(name);
            if (rank == null) {
                return false;
            }

            // Check if rank is default
            if (rank.isDefault()) {
                return false;
            }

            // Delete rank
            dataManager.deleteRank(name);
            rankCache.remove(name.toLowerCase());

            // Update players
            updatePlayersAfterRankDeletion(name);

            // Log action
            auditManager.logAction(
                actor != null ? actor.getUniqueId() : null,
                actor != null ? actor.getName() : "Console",
                AuditLog.ActionType.RANK_DELETE,
                name,
                "Deleted rank " + name,
                configManager.getServerName()
            );

            return true;
        }
    }
    
    /**
     * Updates players after a rank is deleted.
     *
     * @param rankName The name of the deleted rank
     */
    private void updatePlayersAfterRankDeletion(String rankName) {
        plugin.getServer().getOnlinePlayers().forEach(player -> {
            PlayerData playerData = plugin.getDataManager().getPlayerData(player.getUniqueId());
            if (playerData != null) {
                boolean updated = false;

                // Check primary rank
                if (rankName.equals(playerData.getPrimaryRank())) {
                    playerData.setPrimaryRank(defaultRankName);
                    updated = true;
                }

                // Check secondary ranks
                if (playerData.getSecondaryRanks().remove(rankName)) {
                    updated = true;
                }

                // Save if updated
                if (updated) {
                    plugin.getDataManager().savePlayerData(playerData);
                    permissionManager.calculateAndApplyPermissions(player);
                }
            }
        });
    }
    
    /**
     * Sets the default rank.
     *
     * @param name The name of the rank
     * @param actor The player setting the default rank
     * @return True if successful
     */
    public boolean setDefaultRank(String name, Player actor) {
        synchronized (rankLock) {
            // Check if rank exists
            Rank rank = getRank(name);
            if (rank == null) {
                return false;
            }

            // Get current default rank
            Rank currentDefault = getDefaultRank();
            if (currentDefault != null) {
                currentDefault.setDefault(false);
                dataManager.saveRank(currentDefault);
            }

            // Set new default rank
            rank.setDefault(true);
            dataManager.saveRank(rank);
            defaultRankName = name;

            // Log action
            auditManager.logAction(
                actor != null ? actor.getUniqueId() : null,
                actor != null ? actor.getName() : "Console",
                AuditLog.ActionType.RANK_MODIFY,
                name,
                "Set as default rank",
                configManager.getServerName()
            );

            return true;
        }
    }
    
    /**
     * Adds a permission to a rank.
     *
     * @param rankName The name of the rank
     * @param permission The permission to add
     * @param actor The player adding the permission
     * @return True if successful
     */
    public boolean addRankPermission(String rankName, String permission, Player actor) {
        synchronized (rankLock) {
            // Check if rank exists
            Rank rank = getRank(rankName);
            if (rank == null) {
                return false;
            }

            // Add permission
            if (!rank.hasPermission(permission)) {
                rank.addPermission(permission);
                dataManager.saveRank(rank);

                // Update online players with this rank
                updatePlayersAfterRankChange(rankName);

                // Log action
                auditManager.logAction(
                    actor != null ? actor.getUniqueId() : null,
                    actor != null ? actor.getName() : "Console",
                    AuditLog.ActionType.RANK_MODIFY,
                    rankName,
                    "Added permission: " + permission,
                    configManager.getServerName()
                );
            }

            return true;
        }
    }
    
    /**
     * Removes a permission from a rank.
     *
     * @param rankName The name of the rank
     * @param permission The permission to remove
     * @param actor The player removing the permission
     * @return True if successful
     */
    public boolean removeRankPermission(String rankName, String permission, Player actor) {
        synchronized (rankLock) {
            // Check if rank exists
            Rank rank = getRank(rankName);
            if (rank == null) {
                return false;
            }

            // Remove permission
            if (rank.hasPermission(permission)) {
                rank.removePermission(permission);
                dataManager.saveRank(rank);

                // Update online players with this rank
                updatePlayersAfterRankChange(rankName);

                // Log action
                auditManager.logAction(
                    actor != null ? actor.getUniqueId() : null,
                    actor != null ? actor.getName() : "Console",
                    AuditLog.ActionType.RANK_MODIFY,
                    rankName,
                    "Removed permission: " + permission,
                    configManager.getServerName()
                );
            }

            return true;
        }
    }
    
    /**
     * Updates online players after a rank change.
     *
     * @param rankName The name of the changed rank
     */
    private void updatePlayersAfterRankChange(String rankName) {
        plugin.getServer().getOnlinePlayers().forEach(player -> {
            PlayerData playerData = plugin.getDataManager().getPlayerData(player.getUniqueId());
            if (playerData != null && (rankName.equals(playerData.getPrimaryRank()) || 
                                     playerData.getSecondaryRanks().contains(rankName))) {
                permissionManager.calculateAndApplyPermissions(player);
            }
        });
    }
    
    /**
     * Gets all ranks sorted by weight.
     *
     * @return The list of ranks sorted by weight
     */
    public List<Rank> getAllRanks() {
        Map<String, Rank> ranksMap = dataManager.getAllRanks();
        List<Rank> ranks = new ArrayList<>(ranksMap.values());
        
        // Sort by weight (highest first)
        ranks.sort((r1, r2) -> Integer.compare(r2.getWeight(), r1.getWeight()));
        
        return ranks;
    }
    
    /**
     * Gets all rank names sorted by weight.
     *
     * @return The list of rank names sorted by weight
     */
    public List<String> getAllRankNames() {
        return getAllRanks().stream()
                           .map(Rank::getName)
                           .collect(Collectors.toList());
    }
    
    /**
     * Gets the default rank name.
     *
     * @return The default rank name
     */
    public String getDefaultRankName() {
        return defaultRankName;
    }
    
    /**
     * Gets the default rank.
     *
     * @return The default rank
     */
    public Rank getDefaultRank() {
        return dataManager.getRank(defaultRankName);
    }
    
    /**
     * Gets a rank by name.
     *
     * @param name The name of the rank
     * @return The rank, or null if not found
     */
    public Rank getRank(String name) {
        if (name == null) {
            return null;
        }
        
        Rank rank = rankCache.get(name.toLowerCase());
        if (rank == null) {
            synchronized (rankLock) {
                rank = dataManager.getRank(name.toLowerCase());
                if (rank != null) {
                    rankCache.put(name.toLowerCase(), rank);
                }
            }
        }
        return rank;
    }
    
    /**
     * Gets all ranks from the cache.
     *
     * @return A collection of all ranks
     */
    public Collection<Rank> getRanks() {
        return rankCache.values();
    }
    
    /**
     * Adds a permission to a rank.
     *
     * @param rankName The name of the rank
     * @param permission The permission to add
     * @param value Whether the permission is granted (true) or denied (false)
     * @param actor The player adding the permission, or null for console
     * @return Whether the operation was successful
     */
    public boolean addPermissionToRank(String rankName, String permission, boolean value, Player actor) {
        // Check if rank exists
        Rank rank = dataManager.getRank(rankName);
        if (rank == null) {
            plugin.getLogger().warning("Rank not found: " + rankName);
            return false;
        }
        
        // Add the permission
        if (value) {
            rank.addPermission(permission);
        } else {
            rank.addPermission("-" + permission);
        }
        
        // Save the rank
        dataManager.saveRank(rank);
        
        // Log the action
        auditManager.logAction(
            actor != null ? actor.getUniqueId() : null,
            actor != null ? actor.getName() : "Console",
            AuditLog.ActionType.RANK_MODIFY,
            rankName,
            "Added permission " + permission + " with value " + value,
            configManager.getServerName()
        );
        
        // Update players with this rank
        updatePlayersAfterRankChange(rankName);
        
        return true;
    }
    
    /**
     * Removes a permission from a rank.
     *
     * @param rankName The name of the rank
     * @param permission The permission to remove
     * @param actor The player removing the permission, or null for console
     * @return Whether the operation was successful
     */
    public boolean removePermissionFromRank(String rankName, String permission, Player actor) {
        // Check if rank exists
        Rank rank = dataManager.getRank(rankName);
        if (rank == null) {
            plugin.getLogger().warning("Rank not found: " + rankName);
            return false;
        }
        
        // Remove the permission
        rank.removePermission(permission);
        
        // Also remove the negated version if it exists
        if (!permission.startsWith("-")) {
            rank.removePermission("-" + permission);
        } else if (permission.startsWith("-")) {
            rank.removePermission(permission.substring(1));
        }
        
        // Save the rank
        dataManager.saveRank(rank);
        
        // Log the action
        auditManager.logAction(
            actor != null ? actor.getUniqueId() : null,
            actor != null ? actor.getName() : "Console",
            AuditLog.ActionType.RANK_MODIFY,
            rankName,
            "Removed permission " + permission,
            configManager.getServerName()
        );
        
        // Update players with this rank
        updatePlayersAfterRankChange(rankName);
        
        return true;
    }
    
    /**
     * Sets the prefix for a rank.
     *
     * @param rankName The name of the rank
     * @param prefix The prefix to set
     * @return Whether the operation was successful
     */
    public boolean setRankPrefix(String rankName, String prefix) {
        // Check if rank exists
        Rank rank = dataManager.getRank(rankName);
        if (rank == null) {
            plugin.getLogger().warning("Rank not found: " + rankName);
            return false;
        }
        
        // Set the prefix
        rank.setPrefix(prefix);
        
        // Save the rank
        dataManager.saveRank(rank);
        
        // Update players with this rank
        updatePlayersAfterRankChange(rankName);
        
        return true;
    }
    
    /**
     * Sets the color for a rank.
     *
     * @param rankName The name of the rank
     * @param color The color to set
     * @return Whether the operation was successful
     */
    public boolean setRankColor(String rankName, String color) {
        // Check if rank exists
        Rank rank = dataManager.getRank(rankName);
        if (rank == null) {
            plugin.getLogger().warning("Rank not found: " + rankName);
            return false;
        }
        
        // Set the color
        rank.setColor(color);
        
        // Save the rank
        dataManager.saveRank(rank);
        
        // Update players with this rank
        updatePlayersAfterRankChange(rankName);
        
        return true;
    }
    
    /**
     * Sets the suffix for a rank.
     *
     * @param rankName The name of the rank
     * @param suffix The suffix to set
     * @return Whether the operation was successful
     */
    public boolean setRankSuffix(String rankName, String suffix) {
        // Check if rank exists
        Rank rank = dataManager.getRank(rankName);
        if (rank == null) {
            plugin.getLogger().warning("Rank not found: " + rankName);
            return false;
        }
        
        // Set the suffix
        rank.setSuffix(suffix);
        
        // Save the rank
        dataManager.saveRank(rank);
        
        // Update players with this rank
        updatePlayersAfterRankChange(rankName);
        
        return true;
    }
    
    /**
     * Sets the display name for a rank.
     *
     * @param rankName The name of the rank
     * @param displayName The display name to set
     * @return Whether the operation was successful
     */
    public boolean setRankDisplayName(String rankName, String displayName) {
        // Check if rank exists
        Rank rank = dataManager.getRank(rankName);
        if (rank == null) {
            plugin.getLogger().warning("Rank not found: " + rankName);
            return false;
        }
        
        // Set the display name
        rank.setDisplayName(displayName);
        
        // Save the rank
        dataManager.saveRank(rank);
        
        return true;
    }
    
    /**
     * Sets the weight for a rank.
     *
     * @param rankName The name of the rank
     * @param weight The weight to set
     * @return Whether the operation was successful
     */
    public boolean setRankWeight(String rankName, int weight) {
        // Check if rank exists
        Rank rank = dataManager.getRank(rankName);
        if (rank == null) {
            plugin.getLogger().warning("Rank not found: " + rankName);
            return false;
        }
        
        // Set the weight
        rank.setWeight(weight);
        
        // Save the rank
        dataManager.saveRank(rank);
        
        return true;
    }
    
    /**
     * Sets the primary rank for a player.
     *
     * @param uuid The UUID of the player
     * @param rankName The name of the rank to set
     * @param actor The player setting the rank, or null for console
     * @return Whether the operation was successful
     */
    public boolean setPrimaryRank(UUID uuid, String rankName, Player actor) {
        PlayerData playerData = dataManager.getPlayerData(uuid);
        
        if (playerData == null) {
            plugin.getLogger().warning("Failed to retrieve player data for UUID: " + uuid);
            return false;
        }
        
        Rank rank = dataManager.getRank(rankName);
        if (rank == null) {
            plugin.getLogger().warning("Rank not found: " + rankName);
            return false;
        }
        
        String oldRank = playerData.getPrimaryRank();
        playerData.setPrimaryRank(rankName);
        dataManager.savePlayerData(playerData);
        
        // Update permissions if player is online
        Player player = plugin.getServer().getPlayer(uuid);
        if (player != null && player.isOnline()) {
            permissionManager.calculateAndApplyPermissions(player);
        }
        
        // Log the action
        auditManager.logAction(
            actor != null ? actor.getUniqueId() : null,
            actor != null ? actor.getName() : "Console",
            AuditLog.ActionType.RANK_SET,
            playerData.getPlayerName(),
            "Set primary rank to " + rankName + (oldRank != null ? " (was " + oldRank + ")" : ""),
            configManager.getServerName(),
            uuid
        );
        
        return true;
    }
    
    /**
     * Deletes a rank.
     *
     * @param name The name of the rank to delete
     * @return Whether the operation was successful
     */
    public boolean deleteRank(String name) {
        return deleteRank(name, null);
    }
} 