package org.frizzlenpop.frizzlenPerms.models;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Represents a rank in the permission system.
 */
public class Rank {
    
    private final String name;
    private String displayName;
    private String prefix;
    private String suffix;
    private String chatColor;
    private String nameColor;
    private int weight;
    private Set<String> permissions;
    private Map<String, Set<String>> worldPermissions;
    private List<String> inheritance;
    private Map<String, String> metadata;
    private boolean isDefault;
    private boolean canBuild;
    private boolean canDestroy;
    private int ladderPosition;
    private String ladder;
    private int cost;
    private int rankupTime;
    private List<String> rankupRequirements;
    
    /**
     * Creates a new rank with the specified name.
     *
     * @param name The name of the rank
     */
    public Rank(String name) {
        this.name = name;
        this.displayName = name;
        this.prefix = "";
        this.suffix = "";
        this.chatColor = "§f";
        this.nameColor = "§f";
        this.weight = 0;
        this.permissions = ConcurrentHashMap.newKeySet();
        this.worldPermissions = new ConcurrentHashMap<>();
        this.inheritance = new ArrayList<>();
        this.metadata = new ConcurrentHashMap<>();
        this.isDefault = false;
        this.canBuild = true;
        this.canDestroy = true;
        this.ladderPosition = 0;
        this.ladder = "default";
        this.cost = 0;
        this.rankupTime = 0;
        this.rankupRequirements = new ArrayList<>();
    }
    
    /**
     * Creates a new rank with all specified parameters.
     *
     * @param name        The name of the rank
     * @param displayName The display name of the rank
     * @param prefix      The prefix of the rank
     * @param suffix      The suffix of the rank
     * @param color       The color of the rank
     * @param weight      The weight of the rank
     * @param parentRank  The parent rank name (can be null)
     */
    public Rank(String name, String displayName, String prefix, String suffix, String color, int weight, String parentRank) {
        this(name);
        this.displayName = displayName != null ? displayName : name;
        this.prefix = prefix != null ? prefix : "";
        this.suffix = suffix != null ? suffix : "";
        this.chatColor = color != null ? color : "§f";
        this.nameColor = color != null ? color : "§f";
        this.weight = weight;
        if (parentRank != null && !parentRank.isEmpty()) {
            this.inheritance.add(parentRank);
        }
    }
    
    /**
     * Gets the name of the rank.
     *
     * @return The name of the rank
     */
    public String getName() {
        return name;
    }
    
    /**
     * Gets the display name of the rank.
     *
     * @return The display name of the rank
     */
    public String getDisplayName() {
        return displayName;
    }
    
    /**
     * Sets the display name of the rank.
     *
     * @param displayName The display name of the rank
     */
    public void setDisplayName(String displayName) {
        this.displayName = displayName != null ? displayName : name;
    }
    
    /**
     * Gets the prefix of the rank.
     *
     * @return The prefix of the rank
     */
    public String getPrefix() {
        return prefix;
    }
    
    /**
     * Sets the prefix of the rank.
     *
     * @param prefix The prefix of the rank
     */
    public void setPrefix(String prefix) {
        this.prefix = prefix != null ? prefix : "";
    }
    
    /**
     * Gets the suffix of the rank.
     *
     * @return The suffix of the rank
     */
    public String getSuffix() {
        return suffix;
    }
    
    /**
     * Sets the suffix of the rank.
     *
     * @param suffix The suffix of the rank
     */
    public void setSuffix(String suffix) {
        this.suffix = suffix != null ? suffix : "";
    }
    
    /**
     * Gets the chat color of the rank.
     *
     * @return The chat color of the rank
     */
    public String getChatColor() {
        return chatColor;
    }
    
    /**
     * Sets the chat color of the rank.
     *
     * @param chatColor The chat color of the rank
     */
    public void setChatColor(String chatColor) {
        this.chatColor = chatColor != null ? chatColor : "§f";
    }
    
    /**
     * Gets the name color of the rank.
     *
     * @return The name color of the rank
     */
    public String getNameColor() {
        return nameColor;
    }
    
    /**
     * Sets the name color of the rank.
     *
     * @param nameColor The name color of the rank
     */
    public void setNameColor(String nameColor) {
        this.nameColor = nameColor != null ? nameColor : "§f";
    }
    
    /**
     * Gets the weight of the rank.
     *
     * @return The weight of the rank
     */
    public int getWeight() {
        return weight;
    }
    
    /**
     * Sets the weight of the rank.
     *
     * @param weight The weight of the rank
     */
    public void setWeight(int weight) {
        this.weight = weight;
    }
    
    /**
     * Gets the permissions of the rank.
     *
     * @return The permissions of the rank
     */
    public Set<String> getPermissions() {
        return permissions;
    }
    
    /**
     * Sets the permissions of the rank.
     *
     * @param permissions The permissions of the rank
     */
    public void setPermissions(Set<String> permissions) {
        this.permissions = permissions != null ? permissions : ConcurrentHashMap.newKeySet();
    }
    
    /**
     * Adds a permission to the rank.
     *
     * @param permission The permission to add
     */
    public void addPermission(String permission) {
        if (permission != null && !permission.isEmpty()) {
            permissions.add(permission);
        }
    }
    
    /**
     * Removes a permission from the rank.
     *
     * @param permission The permission to remove
     */
    public void removePermission(String permission) {
        if (permission != null) {
            permissions.remove(permission);
        }
    }
    
    /**
     * Checks if the rank has a permission.
     *
     * @param permission The permission to check
     * @return Whether the rank has the permission
     */
    public boolean hasPermission(String permission) {
        return permission != null && permissions.contains(permission);
    }
    
    /**
     * Gets the world permissions of the rank.
     *
     * @return The world permissions of the rank
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
     * Checks if the rank has a permission in a specific world.
     *
     * @param world      The world to check
     * @param permission The permission to check
     * @return Whether the rank has the permission in the world
     */
    public boolean hasWorldPermission(String world, String permission) {
        if (world == null || world.isEmpty() || permission == null || permission.isEmpty()) {
            return false;
        }
        
        Set<String> worldPerms = worldPermissions.get(world);
        return worldPerms != null && worldPerms.contains(permission);
    }
    
    /**
     * Gets the inheritance of the rank.
     *
     * @return The inheritance of the rank
     */
    public List<String> getInheritance() {
        return inheritance;
    }
    
    /**
     * Sets the inheritance of the rank.
     *
     * @param inheritance The inheritance of the rank
     */
    public void setInheritance(List<String> inheritance) {
        this.inheritance = inheritance != null ? inheritance : new ArrayList<>();
    }
    
    /**
     * Adds an inherited rank to the rank.
     *
     * @param rank The rank to inherit
     */
    public void addInheritance(String rank) {
        if (rank != null && !rank.isEmpty() && !inheritance.contains(rank)) {
            inheritance.add(rank);
        }
    }
    
    /**
     * Removes an inherited rank from the rank.
     *
     * @param rank The rank to remove from inheritance
     */
    public void removeInheritance(String rank) {
        if (rank != null) {
            inheritance.remove(rank);
        }
    }
    
    /**
     * Checks if the rank inherits another rank.
     *
     * @param rank The rank to check
     * @return Whether the rank inherits the specified rank
     */
    public boolean inherits(String rank) {
        return rank != null && inheritance.contains(rank);
    }
    
    /**
     * Gets the metadata of the rank.
     *
     * @return The metadata of the rank
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
     * Checks if the rank is the default rank.
     *
     * @return Whether the rank is the default rank
     */
    public boolean isDefault() {
        return isDefault;
    }
    
    /**
     * Sets whether the rank is the default rank.
     *
     * @param isDefault Whether the rank is the default rank
     */
    public void setDefault(boolean isDefault) {
        this.isDefault = isDefault;
    }
    
    /**
     * Checks if the rank can build.
     *
     * @return Whether the rank can build
     */
    public boolean canBuild() {
        return canBuild;
    }
    
    /**
     * Sets whether the rank can build.
     *
     * @param canBuild Whether the rank can build
     */
    public void setCanBuild(boolean canBuild) {
        this.canBuild = canBuild;
    }
    
    /**
     * Checks if the rank can destroy.
     *
     * @return Whether the rank can destroy
     */
    public boolean canDestroy() {
        return canDestroy;
    }
    
    /**
     * Sets whether the rank can destroy.
     *
     * @param canDestroy Whether the rank can destroy
     */
    public void setCanDestroy(boolean canDestroy) {
        this.canDestroy = canDestroy;
    }
    
    /**
     * Gets the position of the rank in its ladder.
     *
     * @return The ladder position of the rank
     */
    public int getLadderPosition() {
        return ladderPosition;
    }
    
    /**
     * Sets the position of the rank in its ladder.
     *
     * @param ladderPosition The ladder position of the rank
     */
    public void setLadderPosition(int ladderPosition) {
        this.ladderPosition = ladderPosition;
    }
    
    /**
     * Gets the ladder of the rank.
     *
     * @return The ladder of the rank
     */
    public String getLadder() {
        return ladder;
    }
    
    /**
     * Sets the ladder of the rank.
     *
     * @param ladder The ladder of the rank
     */
    public void setLadder(String ladder) {
        this.ladder = ladder != null ? ladder : "default";
    }
    
    /**
     * Gets the cost to purchase the rank.
     *
     * @return The cost of the rank
     */
    public int getCost() {
        return cost;
    }
    
    /**
     * Sets the cost to purchase the rank.
     *
     * @param cost The cost of the rank
     */
    public void setCost(int cost) {
        this.cost = cost;
    }
    
    /**
     * Gets the time required to rank up to this rank.
     *
     * @return The rankup time in minutes
     */
    public int getRankupTime() {
        return rankupTime;
    }
    
    /**
     * Sets the time required to rank up to this rank.
     *
     * @param rankupTime The rankup time in minutes
     */
    public void setRankupTime(int rankupTime) {
        this.rankupTime = rankupTime;
    }
    
    /**
     * Gets the additional requirements to rank up to this rank.
     *
     * @return The list of rankup requirements
     */
    public List<String> getRankupRequirements() {
        return rankupRequirements;
    }
    
    /**
     * Sets the additional requirements to rank up to this rank.
     *
     * @param rankupRequirements The list of rankup requirements
     */
    public void setRankupRequirements(List<String> rankupRequirements) {
        this.rankupRequirements = rankupRequirements != null ? rankupRequirements : new ArrayList<>();
    }
    
    /**
     * Adds a requirement to rank up to this rank.
     *
     * @param requirement The requirement to add
     */
    public void addRankupRequirement(String requirement) {
        if (requirement != null && !requirement.isEmpty() && !rankupRequirements.contains(requirement)) {
            rankupRequirements.add(requirement);
        }
    }
    
    /**
     * Removes a requirement to rank up to this rank.
     *
     * @param requirement The requirement to remove
     */
    public void removeRankupRequirement(String requirement) {
        if (requirement != null) {
            rankupRequirements.remove(requirement);
        }
    }
    
    /**
     * Gets the color of the rank (used in GUI).
     *
     * @return The color of the rank
     */
    public String getColor() {
        return chatColor;
    }
    
    /**
     * Sets the color of the rank.
     *
     * @param color The color of the rank
     */
    public void setColor(String color) {
        this.chatColor = color != null ? color : "§f";
    }
    
    /**
     * Gets the parent rank of this rank.
     *
     * @return The parent rank name, or null if this rank has no parent
     */
    public String getParentRank() {
        return inheritance.isEmpty() ? null : inheritance.get(0);
    }
} 