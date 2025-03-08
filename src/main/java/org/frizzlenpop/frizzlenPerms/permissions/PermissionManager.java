package org.frizzlenpop.frizzlenPerms.permissions;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionAttachment;
import org.frizzlenpop.frizzlenPerms.FrizzlenPerms;
import org.frizzlenpop.frizzlenPerms.data.DataManager;
import org.frizzlenpop.frizzlenPerms.models.PlayerData;
import org.frizzlenpop.frizzlenPerms.models.Rank;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages player permissions and attachments.
 */
public class PermissionManager {

    private final FrizzlenPerms plugin;
    private final DataManager dataManager;
    private final Map<UUID, PermissionAttachment> attachments;
    private final Object permissionLock = new Object();

    /**
     * Creates a new PermissionManager instance.
     *
     * @param plugin The plugin instance
     * @param dataManager The data manager
     */
    public PermissionManager(FrizzlenPerms plugin, DataManager dataManager) {
        this.plugin = Objects.requireNonNull(plugin, "Plugin cannot be null");
        this.dataManager = Objects.requireNonNull(dataManager, "DataManager cannot be null");
        this.attachments = new ConcurrentHashMap<>();
    }

    /**
     * Initializes the permission manager.
     */
    public void initialize() {
        plugin.getLogger().info("Permission manager initialized.");
    }

    /**
     * Cleans up resources when the plugin is disabled.
     */
    public void cleanup() {
        // Remove all permission attachments
        for (PermissionAttachment attachment : attachments.values()) {
            if (attachment != null) {
                attachment.remove();
            }
        }
        attachments.clear();
    }

    /**
     * Sets up permissions for a player.
     *
     * @param player The player
     */
    public void setupPermissions(Player player) {
        if (player == null) {
            return;
        }

        UUID uuid = player.getUniqueId();
        
        synchronized (permissionLock) {
            // Remove existing attachment if present
            removeAttachment(uuid);
            
            // Create new attachment
            PermissionAttachment attachment = player.addAttachment(plugin);
            attachments.put(uuid, attachment);
            
            // Get player data
            PlayerData playerData = dataManager.getPlayerData(uuid);
            if (playerData == null) {
                plugin.getLogger().warning("Could not find player data for " + player.getName() + " when setting up permissions.");
                return;
            }
            
            try {
                // Apply rank permissions
                applyRankPermissions(playerData, attachment);
                
                // Apply player-specific permissions
                applyPlayerPermissions(playerData, attachment);
                
                // Apply world-specific permissions
                applyWorldPermissions(playerData, attachment, player.getWorld().getName());
                
                // Apply temporary permissions
                applyTemporaryPermissions(playerData, attachment);
                
                // Recalculate permissions
                player.recalculatePermissions();
                
                plugin.getLogger().fine("Set up permissions for " + player.getName());
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Error setting up permissions for " + player.getName(), e);
            }
        }
    }

    /**
     * Applies rank permissions to a player.
     *
     * @param playerData The player data
     * @param attachment The permission attachment
     */
    private void applyRankPermissions(PlayerData playerData, PermissionAttachment attachment) {
        // Apply permissions from primary rank
        if (playerData.getPrimaryRank() != null) {
            Rank rank = dataManager.getRank(playerData.getPrimaryRank());
            if (rank != null) {
                applyRankPermissionsRecursive(rank, attachment);
            }
        }
        
        // Apply permissions from secondary ranks
        for (String rankName : playerData.getSecondaryRanks()) {
            Rank rank = dataManager.getRank(rankName);
            if (rank != null) {
                applyRankPermissionsRecursive(rank, attachment);
            }
        }
    }

    /**
     * Recursively applies rank permissions including inherited ranks.
     *
     * @param rank The rank
     * @param attachment The permission attachment
     */
    private void applyRankPermissionsRecursive(Rank rank, PermissionAttachment attachment) {
        // Apply permissions from this rank
        for (String permission : rank.getPermissions()) {
            if (permission.startsWith("-")) {
                // Negative permission
                attachment.setPermission(permission.substring(1), false);
            } else {
                // Positive permission
                attachment.setPermission(permission, true);
            }
        }
        
        // Apply permissions from inherited ranks
        for (String inheritedRankName : rank.getInheritance()) {
            Rank inheritedRank = dataManager.getRank(inheritedRankName);
            if (inheritedRank != null && !inheritedRank.getName().equals(rank.getName())) { // Prevent circular inheritance
                applyRankPermissionsRecursive(inheritedRank, attachment);
            }
        }
    }

    /**
     * Applies player-specific permissions.
     *
     * @param playerData The player data
     * @param attachment The permission attachment
     */
    private void applyPlayerPermissions(PlayerData playerData, PermissionAttachment attachment) {
        for (String permission : playerData.getPermissions()) {
            if (permission.startsWith("-")) {
                // Negative permission
                attachment.setPermission(permission.substring(1), false);
            } else {
                // Positive permission
                attachment.setPermission(permission, true);
            }
        }
    }

    /**
     * Applies world-specific permissions.
     *
     * @param playerData The player data
     * @param attachment The permission attachment
     * @param worldName The world name
     */
    private void applyWorldPermissions(PlayerData playerData, PermissionAttachment attachment, String worldName) {
        // Apply world-specific permissions from player data
        Map<String, Set<String>> worldPermissionsMap = playerData.getWorldPermissions();
        if (worldPermissionsMap.containsKey(worldName)) {
            for (String permission : worldPermissionsMap.get(worldName)) {
                if (permission.startsWith("-")) {
                    // Negative permission
                    attachment.setPermission(permission.substring(1), false);
                } else {
                    // Positive permission
                    attachment.setPermission(permission, true);
                }
            }
        }
        
        // Apply world-specific permissions from ranks
        if (playerData.getPrimaryRank() != null) {
            Rank rank = dataManager.getRank(playerData.getPrimaryRank());
            if (rank != null) {
                Map<String, Set<String>> rankWorldPerms = rank.getWorldPermissions();
                if (rankWorldPerms.containsKey(worldName)) {
                    for (String permission : rankWorldPerms.get(worldName)) {
                        if (permission.startsWith("-")) {
                            // Negative permission
                            attachment.setPermission(permission.substring(1), false);
                        } else {
                            // Positive permission
                            attachment.setPermission(permission, true);
                        }
                    }
                }
            }
        }
        
        // Apply world-specific permissions from secondary ranks
        for (String rankName : playerData.getSecondaryRanks()) {
            Rank rank = dataManager.getRank(rankName);
            if (rank != null) {
                Map<String, Set<String>> rankWorldPerms = rank.getWorldPermissions();
                if (rankWorldPerms.containsKey(worldName)) {
                    for (String permission : rankWorldPerms.get(worldName)) {
                        if (permission.startsWith("-")) {
                            // Negative permission
                            attachment.setPermission(permission.substring(1), false);
                        } else {
                            // Positive permission
                            attachment.setPermission(permission, true);
                        }
                    }
                }
            }
        }
    }

    /**
     * Applies temporary permissions.
     *
     * @param playerData The player data
     * @param attachment The permission attachment
     */
    private void applyTemporaryPermissions(PlayerData playerData, PermissionAttachment attachment) {
        long currentTime = System.currentTimeMillis();
        
        // Apply temporary permissions that haven't expired
        for (Map.Entry<String, Long> entry : playerData.getTemporaryPermissions().entrySet()) {
            if (entry.getValue() > currentTime) {
                String permission = entry.getKey();
                if (permission.startsWith("-")) {
                    // Negative permission
                    attachment.setPermission(permission.substring(1), false);
                } else {
                    // Positive permission
                    attachment.setPermission(permission, true);
                }
            }
        }
        
        // Apply temporary ranks that haven't expired
        for (Map.Entry<String, Long> entry : playerData.getTemporaryRanks().entrySet()) {
            if (entry.getValue() > currentTime) {
                Rank rank = dataManager.getRank(entry.getKey());
                if (rank != null) {
                    applyRankPermissionsRecursive(rank, attachment);
                }
            }
        }
    }

    /**
     * Updates permissions for a player.
     *
     * @param uuid The UUID of the player
     */
    public void updatePermissions(UUID uuid) {
        if (uuid == null) {
            return;
        }

        Player player = Bukkit.getPlayer(uuid);
        if (player != null && player.isOnline()) {
            setupPermissions(player);
        }
    }

    /**
     * Updates permissions for all online players.
     */
    public void updateAllPermissions() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            setupPermissions(player);
        }
    }

    /**
     * Gets a player's permission attachment.
     *
     * @param uuid The UUID of the player
     * @return The permission attachment, or null if not found
     */
    public PermissionAttachment getAttachment(UUID uuid) {
        if (uuid == null) {
            return null;
        }
        return attachments.get(uuid);
    }

    /**
     * Removes a player's permission attachment.
     *
     * @param uuid The UUID of the player
     */
    public void removeAttachment(UUID uuid) {
        if (uuid == null) {
            return;
        }

        synchronized (permissionLock) {
            PermissionAttachment attachment = attachments.remove(uuid);
            if (attachment != null) {
                try {
                    attachment.remove();
                } catch (Exception e) {
                    plugin.getLogger().log(Level.WARNING, "Error removing permission attachment for " + uuid, e);
                }
            }
        }
    }

    /**
     * Checks if a player has a rank.
     *
     * @param player The player
     * @param rankName The name of the rank
     * @return True if the player has the rank
     */
    public boolean hasRank(Player player, String rankName) {
        if (player == null || rankName == null) {
            return false;
        }

        PlayerData playerData = dataManager.getPlayerData(player.getUniqueId());
        if (playerData == null) {
            return false;
        }

        return rankName.equals(playerData.getPrimaryRank()) || 
               playerData.getSecondaryRanks().contains(rankName);
    }

    /**
     * Calculates and applies permissions for a player.
     *
     * @param player The player
     */
    public void calculateAndApplyPermissions(Player player) {
        if (player == null || !player.isOnline()) {
            return;
        }

        // Run on the main thread to avoid ConcurrentModificationException
        if (!Bukkit.isPrimaryThread()) {
            Bukkit.getScheduler().runTask(plugin, () -> setupPermissions(player));
        } else {
            setupPermissions(player);
        }
    }
    
    /**
     * Updates the display name and prefix for a player.
     *
     * @param player The player to update
     */
    public void updatePlayerPrefix(Player player) {
        UUID uuid = player.getUniqueId();
        PlayerData playerData = dataManager.getPlayerData(uuid);
        
        if (playerData == null) {
            return;
        }
        
        // Get primary rank
        String primaryRankName = playerData.getPrimaryRank();
        if (primaryRankName == null) {
            return;
        }
        
        Rank primaryRank = dataManager.getRank(primaryRankName);
        if (primaryRank == null) {
            return;
        }
        
        // Update display name with prefix
        String prefix = primaryRank.getPrefix();
        if (prefix != null && !prefix.isEmpty()) {
            String displayName = prefix + player.getName();
            player.setDisplayName(displayName);
            
            // Update player list name if it fits
            if (displayName.length() <= 16) {
                player.setPlayerListName(displayName);
            }
        }
    }

    /**
     * Updates the suffix for a player based on their ranks.
     *
     * @param player The player to update the suffix for
     */
    public void updatePlayerSuffix(Player player) {
        if (player == null || !player.isOnline()) {
            return;
        }

        PlayerData playerData = dataManager.getPlayerData(player.getUniqueId());
        if (playerData == null) {
            return;
        }

        // Get primary rank suffix
        String suffix = "";
        Rank primaryRank = plugin.getRankManager().getRank(playerData.getPrimaryRank());
        if (primaryRank != null && primaryRank.getSuffix() != null) {
            suffix = primaryRank.getSuffix();
        }

        // Apply suffix using chat plugin hook
        plugin.getChatManager().setPlayerSuffix(player, suffix);
    }

    /**
     * Updates the display name for a player based on their ranks.
     *
     * @param player The player to update the display name for
     */
    public void updateDisplayName(Player player) {
        if (player == null || !player.isOnline()) {
            return;
        }

        PlayerData playerData = dataManager.getPlayerData(player.getUniqueId());
        if (playerData == null) {
            return;
        }

        // Get primary rank display name
        String displayName = player.getName();
        Rank primaryRank = plugin.getRankManager().getRank(playerData.getPrimaryRank());
        if (primaryRank != null && primaryRank.getDisplayName() != null) {
            displayName = primaryRank.getDisplayName().replace("%player%", player.getName());
        }

        // Set player's display name
        player.setDisplayName(displayName);
    }
} 