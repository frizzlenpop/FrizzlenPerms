package org.frizzlenpop.frizzlenPerms.sync;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.frizzlenpop.frizzlenPerms.FrizzlenPerms;
import org.frizzlenpop.frizzlenPerms.models.PlayerData;
import org.frizzlenpop.frizzlenPerms.utils.MessageUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

/**
 * Manages synchronization between Minecraft and Discord.
 */
public class SyncManager {
    
    private final FrizzlenPerms plugin;
    private final Map<String, UUID> linkCodes;
    private final Map<String, Long> linkCodeExpiry;
    private BukkitTask cleanupTask;
    private final Random random;
    
    /**
     * Creates a new SyncManager.
     *
     * @param plugin The plugin instance
     */
    public SyncManager(FrizzlenPerms plugin) {
        this.plugin = plugin;
        this.linkCodes = new HashMap<>();
        this.linkCodeExpiry = new HashMap<>();
        this.random = new Random();
    }
    
    /**
     * Initializes the sync manager.
     */
    public void initialize() {
        // Start cleanup task
        startCleanupTask();
        
        plugin.getLogger().info("Sync manager initialized.");
    }
    
    /**
     * Starts the cleanup task.
     */
    private void startCleanupTask() {
        // Cancel existing task
        if (cleanupTask != null) {
            cleanupTask.cancel();
        }
        
        // Start new task
        cleanupTask = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, this::cleanupExpiredCodes, 20 * 60, 20 * 60);
    }
    
    /**
     * Shuts down the sync manager.
     */
    public void shutdown() {
        // Cancel cleanup task
        if (cleanupTask != null) {
            cleanupTask.cancel();
            cleanupTask = null;
        }
        
        // Clear codes
        linkCodes.clear();
        linkCodeExpiry.clear();
    }
    
    /**
     * Reloads the sync manager.
     */
    public void reload() {
        shutdown();
        initialize();
    }
    
    /**
     * Generates a link code for a player.
     *
     * @param player The player
     * @return The link code
     */
    public String generateLinkCode(Player player) {
        // Generate code
        String code = generateRandomCode();
        
        // Store code
        linkCodes.put(code, player.getUniqueId());
        linkCodeExpiry.put(code, System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(5));
        
        // Send message to player
        player.sendMessage(MessageUtils.formatMessage("&aYour Discord link code is: &e" + code));
        player.sendMessage(MessageUtils.formatMessage("&aUse &e/link " + code + " &ain Discord to link your account."));
        player.sendMessage(MessageUtils.formatMessage("&aThis code will expire in 5 minutes."));
        
        return code;
    }
    
    /**
     * Generates a random code.
     *
     * @return The random code
     */
    private String generateRandomCode() {
        // Generate 6-digit code
        int code = 100000 + random.nextInt(900000);
        return String.valueOf(code);
    }
    
    /**
     * Gets the player UUID from a link code.
     *
     * @param code The link code
     * @return The player UUID, or null if the code is invalid or expired
     */
    public UUID getPlayerUuidFromCode(String code) {
        // Check if code exists
        if (!linkCodes.containsKey(code)) {
            return null;
        }
        
        // Check if code is expired
        if (System.currentTimeMillis() > linkCodeExpiry.get(code)) {
            // Remove expired code
            linkCodes.remove(code);
            linkCodeExpiry.remove(code);
            return null;
        }
        
        // Get player UUID
        UUID playerUuid = linkCodes.get(code);
        
        // Remove used code
        linkCodes.remove(code);
        linkCodeExpiry.remove(code);
        
        return playerUuid;
    }
    
    /**
     * Cleans up expired codes.
     */
    private void cleanupExpiredCodes() {
        try {
            long now = System.currentTimeMillis();
            
            // Find expired codes
            linkCodeExpiry.entrySet().removeIf(entry -> {
                if (now > entry.getValue()) {
                    linkCodes.remove(entry.getKey());
                    return true;
                }
                return false;
            });
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error cleaning up expired codes", e);
        }
    }
    
    /**
     * Syncs a player's ranks with Discord.
     *
     * @param player The player
     * @return True if the sync was successful
     */
    public boolean syncPlayerWithDiscord(Player player) {
        // Check if Discord is enabled
        if (!plugin.getConfigManager().isDiscordEnabled()) {
            return false;
        }
        
        // Get player data
        PlayerData playerData = plugin.getDataManager().getPlayerData(player.getUniqueId());
        if (playerData == null) {
            return false;
        }
        
        // Check if player is linked
        String discordId = playerData.getDiscordId();
        if (discordId == null || discordId.isEmpty()) {
            return false;
        }
        
        // Sync player roles
        return plugin.getDiscordManager().syncPlayerRoles(player.getUniqueId());
    }
} 