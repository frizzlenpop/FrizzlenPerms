package org.frizzlenpop.frizzlenPerms.models;

/**
 * Represents a temporary rank with an expiration time.
 */
public class TempRank {
    private final String rankName;
    private final long expirationTime;

    /**
     * Creates a new temporary rank.
     *
     * @param rankName The name of the rank
     * @param expirationTime The expiration time in milliseconds since epoch
     */
    public TempRank(String rankName, long expirationTime) {
        this.rankName = rankName;
        this.expirationTime = expirationTime;
    }

    /**
     * Gets the name of the rank.
     *
     * @return The rank name
     */
    public String getRankName() {
        return rankName;
    }

    /**
     * Gets the expiration time of the rank.
     *
     * @return The expiration time in milliseconds since epoch
     */
    public long getExpirationTime() {
        return expirationTime;
    }
} 