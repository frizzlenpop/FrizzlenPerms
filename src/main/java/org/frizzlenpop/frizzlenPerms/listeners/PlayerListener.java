package org.frizzlenpop.frizzlenPerms.listeners;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.frizzlenpop.frizzlenPerms.FrizzlenPerms;
import org.frizzlenpop.frizzlenPerms.data.DataManager;
import org.frizzlenpop.frizzlenPerms.models.PlayerData;
import org.frizzlenpop.frizzlenPerms.models.Rank;
import org.frizzlenpop.frizzlenPerms.permissions.PermissionManager;
import org.frizzlenpop.frizzlenPerms.ranks.RankManager;

import java.util.UUID;

/**
 * Handles player-related events such as join and quit.
 */
public class PlayerListener implements Listener {
    
    private final FrizzlenPerms plugin;
    private final DataManager dataManager;
    private final PermissionManager permissionManager;
    private final RankManager rankManager;
    
    /**
     * Creates a new PlayerListener instance.
     *
     * @param plugin The plugin instance
     * @param dataManager The data manager instance
     * @param permissionManager The permission manager instance
     * @param rankManager The rank manager instance
     */
    public PlayerListener(FrizzlenPerms plugin, DataManager dataManager, 
                        PermissionManager permissionManager, RankManager rankManager) {
        this.plugin = plugin;
        this.dataManager = dataManager;
        this.permissionManager = permissionManager;
        this.rankManager = rankManager;
    }
    
    /**
     * Handles player pre-login events to prepare player data.
     *
     * @param event The pre-login event
     */
    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerPreLogin(AsyncPlayerPreLoginEvent event) {
        UUID uuid = event.getUniqueId();
        String name = event.getName();
        
        // Get player data or create if not exists
        PlayerData playerData = dataManager.getPlayerData(uuid);
        if (playerData == null) {
            playerData = createNewPlayerData(uuid, name);
        } else if (!playerData.getPlayerName().equals(name)) {
            // Update name if changed
            playerData.setPlayerName(name);
            dataManager.savePlayerData(playerData);
            plugin.getLogger().info("Updated name for " + uuid + " to " + name);
        }
    }
    
    /**
     * Handles player join events to set up permissions.
     *
     * @param event The join event
     */
    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        
        // Get player data
        PlayerData playerData = dataManager.getPlayerData(uuid);
        if (playerData == null) {
            // Shouldn't happen due to pre-login handler, but just in case
            playerData = createNewPlayerData(uuid, player.getName());
        }
        
        // Update last login time
        playerData.setLastLogin(System.currentTimeMillis());
        dataManager.savePlayerData(playerData);
        
        // Set up permissions
        permissionManager.setupPermissions(player);
        
        // Send welcome message if configured
        // TODO: Implement welcome message
    }
    
    /**
     * Handles player quit events to update last seen time.
     *
     * @param event The quit event
     */
    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        
        // Update last seen time
        PlayerData playerData = dataManager.getPlayerData(uuid);
        if (playerData != null) {
            playerData.setLastSeen(System.currentTimeMillis());
            dataManager.savePlayerData(playerData);
        }
    }
    
    /**
     * Creates a new player data entry for a player.
     *
     * @param uuid The UUID of the player
     * @param name The name of the player
     * @return The created player data
     */
    private PlayerData createNewPlayerData(UUID uuid, String name) {
        plugin.getLogger().info("Creating new player data for " + name + " (" + uuid + ")");
        
        // Create new player data
        PlayerData playerData = new PlayerData(uuid, name);
        
        // Set default rank
        Rank defaultRank = rankManager.getDefaultRank();
        if (defaultRank != null) {
            playerData.setPrimaryRank(defaultRank.getName());
        }
        
        // Set current time as last seen and last login
        long currentTime = System.currentTimeMillis();
        playerData.setLastSeen(currentTime);
        playerData.setLastLogin(currentTime);
        
        // Save the player data
        dataManager.savePlayerData(playerData);
        
        return playerData;
    }
} 