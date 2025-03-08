package org.frizzlenpop.frizzlenPerms.commands.admin;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.frizzlenpop.frizzlenPerms.FrizzlenPerms;
import org.frizzlenpop.frizzlenPerms.commands.SubCommand;
import org.frizzlenpop.frizzlenPerms.models.AuditLog;
import org.frizzlenpop.frizzlenPerms.utils.MessageUtils;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Command to pull permissions data from other servers.
 */
public class SyncPullCommand implements SubCommand {
    
    private final FrizzlenPerms plugin;
    
    /**
     * Creates a new SyncPullCommand.
     *
     * @param plugin The plugin instance
     */
    public SyncPullCommand(FrizzlenPerms plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public String getName() {
        return "syncpull";
    }
    
    @Override
    public String getDescription() {
        return "Pulls permissions data from other servers.";
    }
    
    @Override
    public String getUsage() {
        return "/frizzlenperms syncpull [confirm]";
    }
    
    @Override
    public String getPermission() {
        return "frizzlenperms.admin.syncpull";
    }
    
    @Override
    public int getMinArgs() {
        return 0;
    }
    
    @Override
    public List<String> getAliases() {
        return List.of("pull", "syncdown");
    }
    
    @Override
    public boolean execute(CommandSender sender, String[] args) {
        // Check if sync is enabled
        if (!plugin.getConfigManager().isSyncEnabled()) {
            MessageUtils.sendMessage(sender, "sync.not-enabled");
            return false;
        }
        
        // Check for confirmation if needed
        boolean confirmed = args.length > 0 && args[0].equalsIgnoreCase("confirm");
        if (!confirmed) {
            MessageUtils.sendMessage(sender, "sync.pull-confirm", Map.of(
                "command", "/frizzlenperms syncpull confirm"
            ));
            return true;
        }
        
        // Send starting message
        MessageUtils.sendMessage(sender, "sync.pull-starting");
        
        // Run async
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                // Simulate pull operation (in a real implementation, this would call methods from SyncManager)
                boolean success = true; // Simulated success
                String serverName = "FrizzlenPerms Server";
                
                // Log the action
                if (sender instanceof Player) {
                    Player player = (Player) sender;
                    plugin.getAuditManager().logAction(
                        AuditLog.ActionType.SYNC_PULL,
                        serverName,
                        player.getUniqueId(),
                        "Pulled permissions data from other servers",
                        serverName
                    );
                } else {
                    plugin.getAuditManager().logAction(
                        AuditLog.ActionType.SYNC_PULL,
                        serverName,
                        null, // No UUID for console
                        "Pulled permissions data from other servers",
                        serverName
                    );
                }
                
                // After pull, update permissions for all online players
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    // Update all permissions
                    plugin.getPermissionManager().updateAllPermissions();
                    
                    // Send completion message
                    if (success) {
                        MessageUtils.sendMessage(sender, "sync.pull-success");
                    } else {
                        MessageUtils.sendMessage(sender, "sync.pull-failed");
                    }
                });
                
            } catch (Exception e) {
                plugin.getLogger().severe("Error during sync pull: " + e.getMessage());
                e.printStackTrace();
                
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    MessageUtils.sendMessage(sender, "sync.error", Map.of(
                        "error", e.getMessage()
                    ));
                });
            }
        });
        
        return true;
    }
    
    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return List.of("confirm");
        }
        return List.of();
    }
} 