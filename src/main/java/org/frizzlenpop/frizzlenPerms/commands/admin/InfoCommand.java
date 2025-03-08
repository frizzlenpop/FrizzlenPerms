package org.frizzlenpop.frizzlenPerms.commands.admin;

import org.bukkit.command.CommandSender;
import org.frizzlenpop.frizzlenPerms.FrizzlenPerms;
import org.frizzlenpop.frizzlenPerms.commands.SubCommand;
import org.frizzlenpop.frizzlenPerms.models.Rank;
import org.frizzlenpop.frizzlenPerms.utils.MessageUtils;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Command to display information about the permissions system.
 */
public class InfoCommand implements SubCommand {
    
    private final FrizzlenPerms plugin;
    
    /**
     * Creates a new InfoCommand.
     *
     * @param plugin The plugin instance
     */
    public InfoCommand(FrizzlenPerms plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public String getName() {
        return "info";
    }
    
    @Override
    public String getDescription() {
        return "Shows information about the permissions system.";
    }
    
    @Override
    public String getUsage() {
        return "/frizzlenperms info";
    }
    
    @Override
    public String getPermission() {
        return "frizzlenperms.admin.info";
    }
    
    @Override
    public int getMinArgs() {
        return 0;
    }
    
    @Override
    public List<String> getAliases() {
        return List.of("stats", "information");
    }
    
    @Override
    public boolean execute(CommandSender sender, String[] args) {
        // Run asynchronously to not block the main thread
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                // Get database stats
                int rankCount = plugin.getRankManager().getRanks().size();
                
                // Count total permissions across all ranks
                AtomicInteger permissionCount = new AtomicInteger(0);
                plugin.getRankManager().getRanks().forEach(rank -> {
                    if (rank != null && rank.getPermissions() != null) {
                        permissionCount.addAndGet(rank.getPermissions().size());
                    }
                });
                
                // Count players with data
                int playerCount = plugin.getDataManager().getAllPlayerData().size();
                
                // Get default rank
                String defaultRankName = "None";
                for (Rank rank : plugin.getRankManager().getRanks()) {
                    if (rank != null && rank.isDefault()) {
                        defaultRankName = rank.getDisplayName() != null ? rank.getDisplayName() : rank.getName();
                        break;
                    }
                }
                
                // Get the storage type
                String storageType = plugin.getConfigManager().getStorageType();
                if (storageType == null || storageType.isEmpty()) {
                    storageType = "Unknown";
                }
                
                // Is sync enabled
                boolean syncEnabled = plugin.getConfigManager().isSyncEnabled();
                
                // Send results to player
                final String finalDefaultRankName = defaultRankName;
                final String finalStorageType = storageType;
                
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    try {
                        // Header
                        MessageUtils.sendMessage(sender, "admin.info-header");
                        
                        // Database stats
                        MessageUtils.sendMessage(sender, "admin.info-ranks", Map.of(
                            "count", String.valueOf(rankCount),
                            "permissions", String.valueOf(permissionCount.get())
                        ));
                        
                        // Player stats
                        MessageUtils.sendMessage(sender, "admin.info-players", Map.of(
                            "count", String.valueOf(playerCount)
                        ));
                        
                        // Default rank
                        MessageUtils.sendMessage(sender, "admin.info-default-rank", Map.of(
                            "rank", finalDefaultRankName
                        ));
                        
                        // Storage info
                        MessageUtils.sendMessage(sender, "admin.info-storage", Map.of(
                            "type", finalStorageType,
                            "sync", syncEnabled ? "Enabled" : "Disabled"
                        ));
                    } catch (Exception e) {
                        plugin.getLogger().severe("Error sending info messages: " + e.getMessage());
                        MessageUtils.sendMessage(sender, "error.internal-error");
                    }
                });
            } catch (Exception e) {
                plugin.getLogger().severe("Error gathering info data: " + e.getMessage());
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    MessageUtils.sendMessage(sender, "error.internal-error");
                });
            }
        });
        
        return true;
    }
    
    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        return List.of();
    }
} 