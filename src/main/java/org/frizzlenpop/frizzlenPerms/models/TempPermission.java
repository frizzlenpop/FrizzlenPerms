package org.frizzlenpop.frizzlenPerms.models;

/**
 * Represents a temporary permission with an expiration time.
 */
public class TempPermission {
    private final String permission;
    private final long expirationTime;

    /**
     * Creates a new temporary permission.
     *
     * @param permission The permission string
     * @param expirationTime The expiration time in milliseconds since epoch
     */
    public TempPermission(String permission, long expirationTime) {
        this.permission = permission;
        this.expirationTime = expirationTime;
    }

    /**
     * Gets the permission string.
     *
     * @return The permission string
     */
    public String getPermission() {
        return permission;
    }

    /**
     * Gets the expiration time of the permission.
     *
     * @return The expiration time in milliseconds since epoch
     */
    public long getExpirationTime() {
        return expirationTime;
    }
} 