package org.frizzlenpop.frizzlenPerms.models;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Represents a player's permission data.
 */
public class PlayerData {
    
    private final UUID uuid;
    private String playerName;
    private String primaryRank;
    private List<String> secondaryRanks;
    private Set<String> permissions;
    private Map<String, Set<String>> worldPermissions;
    private Map<String, Long> temporaryRanks;
    private Map<String, Long> temporaryPermissions;
    private String discordId;
    private long lastSeen;
    private long lastLogin;
    private Map<String, String> metadata;
    
    /**
     * Creates a new PlayerData with the specified UUID and name.
     *
     * @param uuid       The UUID of the player
     * @param playerName The name of the player
     */
    public PlayerData(UUID uuid, String playerName) {
        this.uuid = uuid;
        this.playerName = playerName;
        this.secondaryRanks = new ArrayList<>();
        this.permissions = ConcurrentHashMap.newKeySet();
        this.worldPermissions = new ConcurrentHashMap<>();
        this.temporaryRanks = new ConcurrentHashMap<>();
        this.temporaryPermissions = new ConcurrentHashMap<>();
        this.metadata = new ConcurrentHashMap<>();
    }
    
    /**
     * Gets the UUID of the player.
     *
     * @return The UUID of the player
     */
    public UUID getUuid() {
        return uuid;
    }
    
    /**
     * Gets the name of the player.
     *
     * @return The name of the player
     */
    public String getPlayerName() {
        return playerName;
    }
    
    /**
     * Sets the name of the player.
     *
     * @param playerName The name of the player
     */
    public void setPlayerName(String playerName) {
        this.playerName = playerName;
    }
    
    /**
     * Gets the primary rank of the player.
     *
     * @return The primary rank of the player
     */
    public String getPrimaryRank() {
        return primaryRank;
    }
    
    /**
     * Sets the primary rank of the player.
     *
     * @param primaryRank The primary rank of the player
     */
    public void setPrimaryRank(String primaryRank) {
        this.primaryRank = primaryRank;
    }
    
    /**
     * Gets the secondary ranks of the player.
     *
     * @return The secondary ranks of the player
     */
    public List<String> getSecondaryRanks() {
        return secondaryRanks;
    }
    
    /**
     * Sets the secondary ranks of the player.
     *
     * @param secondaryRanks The secondary ranks of the player
     */
    public void setSecondaryRanks(List<String> secondaryRanks) {
        this.secondaryRanks = secondaryRanks != null ? secondaryRanks : new ArrayList<>();
    }
    
    /**
     * Adds a secondary rank to the player.
     *
     * @param rank The rank to add
     */
    public void addSecondaryRank(String rank) {
        if (rank != null && !rank.isEmpty() && !secondaryRanks.contains(rank)) {
            secondaryRanks.add(rank);
        }
    }
    
    /**
     * Removes a secondary rank from the player.
     *
     * @param rank The rank to remove
     */
    public void removeSecondaryRank(String rank) {
        if (rank != null) {
            secondaryRanks.remove(rank);
        }
    }
    
    /**
     * Gets the permissions of the player.
     *
     * @return The permissions of the player
     */
    public Set<String> getPermissions() {
        return permissions;
    }
    
    /**
     * Sets the permissions of the player.
     *
     * @param permissions The permissions of the player
     */
    public void setPermissions(Set<String> permissions) {
        this.permissions = permissions != null ? permissions : ConcurrentHashMap.newKeySet();
    }
    
    /**
     * Adds a permission to the player.
     *
     * @param permission The permission to add
     */
    public void addPermission(String permission) {
        if (permission != null && !permission.isEmpty()) {
            permissions.add(permission);
        }
    }
    
    /**
     * Removes a permission from the player.
     *
     * @param permission The permission to remove
     */
    public void removePermission(String permission) {
        if (permission != null) {
            permissions.remove(permission);
        }
    }
    
    /**
     * Checks if the player has a permission.
     *
     * @param permission The permission to check
     * @return Whether the player has the permission
     */
    public boolean hasPermission(String permission) {
        return permission != null && permissions.contains(permission);
    }
    
    /**
     * Gets the world permissions of the player.
     *
     * @return The world permissions of the player
     */
    public Map<String, Set<String>> getWorldPermissions() {
        return worldPermissions;
    }
    
    /**
     * Gets the permissions for a specific world.
     *
     * @param world The world to get permissions for
     * @return The permissions for the world
     */
    public Set<String> getWorldPermissions(String world) {
        return worldPermissions.getOrDefault(world, Collections.emptySet());
    }
    
    /**
     * Sets the permissions for a specific world.
     *
     * @param world       The world to set permissions for
     * @param permissions The permissions for the world
     */
    public void setWorldPermissions(String world, Set<String> permissions) {
        if (world != null && !world.isEmpty()) {
            if (permissions != null && !permissions.isEmpty()) {
                worldPermissions.put(world, new HashSet<>(permissions));
            } else {
                worldPermissions.remove(world);
            }
        }
    }
    
    /**
     * Adds a permission for a specific world.
     *
     * @param world      The world to add the permission for
     * @param permission The permission to add
     */
    public void addWorldPermission(String world, String permission) {
        if (world != null && !world.isEmpty() && permission != null && !permission.isEmpty()) {
            worldPermissions.computeIfAbsent(world, k -> ConcurrentHashMap.newKeySet()).add(permission);
        }
    }
    
    /**
     * Removes a permission for a specific world.
     *
     * @param world      The world to remove the permission from
     * @param permission The permission to remove
     */
    public void removeWorldPermission(String world, String permission) {
        if (world != null && !world.isEmpty() && permission != null && !permission.isEmpty()) {
            Set<String> worldPerms = worldPermissions.get(world);
            if (worldPerms != null) {
                worldPerms.remove(permission);
                if (worldPerms.isEmpty()) {
                    worldPermissions.remove(world);
                }
            }
        }
    }
    
    /**
     * Checks if the player has a permission in a specific world.
     *
     * @param world      The world to check
     * @param permission The permission to check
     * @return Whether the player has the permission in the world
     */
    public boolean hasWorldPermission(String world, String permission) {
        if (world == null || world.isEmpty() || permission == null || permission.isEmpty()) {
            return false;
        }
        
        Set<String> worldPerms = worldPermissions.get(world);
        return worldPerms != null && worldPerms.contains(permission);
    }
    
    /**
     * Gets the temporary ranks of the player.
     *
     * @return The temporary ranks of the player
     */
    public Map<String, Long> getTemporaryRanks() {
        return temporaryRanks;
    }
    
    /**
     * Gets the expiration time of a temporary rank.
     *
     * @param rank The rank to get the expiration time for
     * @return The expiration time of the rank, or 0 if not temporary
     */
    public long getTemporaryRankExpiration(String rank) {
        return temporaryRanks.getOrDefault(rank, 0L);
    }
    
    /**
     * Checks if a rank is temporary.
     *
     * @param rank The rank to check
     * @return Whether the rank is temporary
     */
    public boolean isTemporaryRank(String rank) {
        return temporaryRanks.containsKey(rank);
    }
    
    /**
     * Adds a temporary rank to the player.
     *
     * @param rank       The rank to add
     * @param expiration The expiration time of the rank
     */
    public void addTemporaryRank(String rank, long expiration) {
        if (rank != null && !rank.isEmpty() && expiration > 0) {
            temporaryRanks.put(rank, expiration);
        }
    }
    
    /**
     * Removes a temporary rank from the player.
     *
     * @param rank The rank to remove
     */
    public void removeTemporaryRank(String rank) {
        if (rank != null) {
            temporaryRanks.remove(rank);
        }
    }
    
    /**
     * Gets the temporary permissions of the player.
     *
     * @return The temporary permissions of the player
     */
    public Map<String, Long> getTemporaryPermissions() {
        return temporaryPermissions;
    }
    
    /**
     * Gets the expiration time of a temporary permission.
     *
     * @param permission The permission to get the expiration time for
     * @return The expiration time of the permission, or 0 if not temporary
     */
    public long getTemporaryPermissionExpiration(String permission) {
        return temporaryPermissions.getOrDefault(permission, 0L);
    }
    
    /**
     * Checks if a permission is temporary.
     *
     * @param permission The permission to check
     * @return Whether the permission is temporary
     */
    public boolean isTemporaryPermission(String permission) {
        return temporaryPermissions.containsKey(permission);
    }
    
    /**
     * Adds a temporary permission to the player.
     *
     * @param permission The permission to add
     * @param expiration The expiration time of the permission
     */
    public void addTemporaryPermission(String permission, long expiration) {
        if (permission != null && !permission.isEmpty() && expiration > 0) {
            temporaryPermissions.put(permission, expiration);
        }
    }
    
    /**
     * Removes a temporary permission from the player.
     *
     * @param permission The permission to remove
     */
    public void removeTemporaryPermission(String permission) {
        if (permission != null) {
            temporaryPermissions.remove(permission);
        }
    }
    
    /**
     * Gets the Discord ID of the player.
     *
     * @return The Discord ID of the player
     */
    public String getDiscordId() {
        return discordId;
    }
    
    /**
     * Sets the Discord ID of the player.
     *
     * @param discordId The Discord ID of the player
     */
    public void setDiscordId(String discordId) {
        this.discordId = discordId;
    }
    
    /**
     * Gets the last seen time of the player.
     *
     * @return The last seen time of the player
     */
    public long getLastSeen() {
        return lastSeen;
    }
    
    /**
     * Sets the last seen time of the player.
     *
     * @param lastSeen The last seen time of the player
     */
    public void setLastSeen(long lastSeen) {
        this.lastSeen = lastSeen;
    }
    
    /**
     * Gets the last login time of the player.
     *
     * @return The last login time of the player
     */
    public long getLastLogin() {
        return lastLogin;
    }
    
    /**
     * Sets the last login time of the player.
     *
     * @param lastLogin The last login time of the player
     */
    public void setLastLogin(long lastLogin) {
        this.lastLogin = lastLogin;
    }
    
    /**
     * Gets the metadata of the player.
     *
     * @return The metadata of the player
     */
    public Map<String, String> getMetadata() {
        return metadata;
    }
    
    /**
     * Gets a metadata value.
     *
     * @param key The key of the metadata
     * @return The value of the metadata, or null if not found
     */
    public String getMetadata(String key) {
        return metadata.get(key);
    }
    
    /**
     * Sets a metadata value.
     *
     * @param key   The key of the metadata
     * @param value The value of the metadata
     */
    public void setMetadata(String key, String value) {
        if (key != null && !key.isEmpty()) {
            if (value != null) {
                metadata.put(key, value);
            } else {
                metadata.remove(key);
            }
        }
    }
    
    /**
     * Removes a metadata value.
     *
     * @param key The key of the metadata to remove
     */
    public void removeMetadata(String key) {
        if (key != null) {
            metadata.remove(key);
        }
    }
    
    /**
     * Clears all metadata.
     */
    public void clearMetadata() {
        metadata.clear();
    }
    
    /**
     * Sets the prefix for the player.
     *
     * @param prefix The prefix to set
     */
    public void setPrefix(String prefix) {
        setMetadata("prefix", prefix);
    }

    /**
     * Sets the suffix for the player.
     *
     * @param suffix The suffix to set
     */
    public void setSuffix(String suffix) {
        setMetadata("suffix", suffix);
    }

    /**
     * Sets the chat color for the player.
     *
     * @param chatColor The chat color to set
     */
    public void setChatColor(String chatColor) {
        setMetadata("chatColor", chatColor);
    }

    /**
     * Sets the name color for the player.
     *
     * @param nameColor The name color to set
     */
    public void setNameColor(String nameColor) {
        setMetadata("nameColor", nameColor);
    }

    /**
     * Adds a rank to the player.
     *
     * @param rank The rank to add
     */
    public void addRank(String rank) {
        if (primaryRank == null) {
            setPrimaryRank(rank);
        } else {
            addSecondaryRank(rank);
        }
    }
} 