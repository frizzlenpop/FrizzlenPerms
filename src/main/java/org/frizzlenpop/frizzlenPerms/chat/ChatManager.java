package org.frizzlenpop.frizzlenPerms.chat;

import org.bukkit.entity.Player;
import org.frizzlenpop.frizzlenPerms.FrizzlenPerms;

import java.util.Objects;

/**
 * Manages chat-related functionality and integrations with chat plugins.
 */
public class ChatManager {
    
    private final FrizzlenPerms plugin;
    
    /**
     * Creates a new ChatManager instance.
     *
     * @param plugin The plugin instance
     */
    public ChatManager(FrizzlenPerms plugin) {
        this.plugin = Objects.requireNonNull(plugin, "Plugin cannot be null");
    }
    
    /**
     * Sets a player's prefix.
     *
     * @param player The player to set the prefix for
     * @param prefix The prefix to set
     */
    public void setPlayerPrefix(Player player, String prefix) {
        // TODO: Implement chat plugin integration (e.g., Vault, LuckPerms API, etc.)
        // For now, just store the prefix in the player's metadata
        player.setMetadata("frizzlenperms.prefix", new org.bukkit.metadata.FixedMetadataValue(plugin, prefix));
    }
    
    /**
     * Sets a player's suffix.
     *
     * @param player The player to set the suffix for
     * @param suffix The suffix to set
     */
    public void setPlayerSuffix(Player player, String suffix) {
        // TODO: Implement chat plugin integration (e.g., Vault, LuckPerms API, etc.)
        // For now, just store the suffix in the player's metadata
        player.setMetadata("frizzlenperms.suffix", new org.bukkit.metadata.FixedMetadataValue(plugin, suffix));
    }
    
    /**
     * Gets a player's prefix.
     *
     * @param player The player to get the prefix for
     * @return The player's prefix, or an empty string if none is set
     */
    public String getPlayerPrefix(Player player) {
        if (!player.hasMetadata("frizzlenperms.prefix")) {
            return "";
        }
        return player.getMetadata("frizzlenperms.prefix").get(0).asString();
    }
    
    /**
     * Gets a player's suffix.
     *
     * @param player The player to get the suffix for
     * @return The player's suffix, or an empty string if none is set
     */
    public String getPlayerSuffix(Player player) {
        if (!player.hasMetadata("frizzlenperms.suffix")) {
            return "";
        }
        return player.getMetadata("frizzlenperms.suffix").get(0).asString();
    }
} 