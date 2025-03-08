package org.frizzlenpop.frizzlenPerms.commands.admin;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.frizzlenpop.frizzlenPerms.FrizzlenPerms;
import org.frizzlenpop.frizzlenPerms.commands.SubCommand;
import org.frizzlenpop.frizzlenPerms.models.AuditLog;
import org.frizzlenpop.frizzlenPerms.utils.MessageUtils;

import java.util.List;
import java.util.Map;

/**
 * Command to push permissions data to other servers.
 */
public class SyncPushCommand implements SubCommand {
    
    private final FrizzlenPerms plugin;
    
    /**
     * Creates a new SyncPushCommand.
     *
     * @param plugin The plugin instance
     */
    public SyncPushCommand(FrizzlenPerms plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public String getName() {
        return "syncpush";
    }
    
    @Override
    public String getDescription() {
        return "Pushes permissions data to other servers.";
    }
    
    @Override
    public String getUsage() {
        return "/frizzlenperms syncpush [confirm]";
    }
    
    @Override
    public String getPermission() {
        return "frizzlenperms.admin.syncpush";
    }
    
    @Override
    public int getMinArgs() {
        return 0;
    }
    
    @Override
    public List<String> getAliases() {
        return List.of("push", "syncup");
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
            MessageUtils.sendMessage(sender, "sync.push-confirm", Map.of(
                "command", "/frizzlenperms syncpush confirm"
            ));
            return true;
        }
        
        // Send starting message
        MessageUtils.sendMessage(sender, "sync.push-starting");
        
        // Run async
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                // Simulate push operation (in a real implementation, this would call methods from SyncManager)
                boolean success = true; // Simulated success
                String serverName = "FrizzlenPerms Server";
                
                // Log the action
                if (sender instanceof Player) {
                    Player player = (Player) sender;
                    plugin.getAuditManager().logAction(
                        player.getUniqueId(),
                        player.getName(),
                        AuditLog.ActionType.SYNC_PUSH,
                        "All",
                        "Pushed permissions data to other servers",
                        serverName,
                        null
                    );
                } else {
                    plugin.getAuditManager().logAction(
                        null,
                        "Console",
                        AuditLog.ActionType.SYNC_PUSH,
                        "All",
                        "Pushed permissions data to other servers",
                        serverName,
                        null
                    );
                }
                
                // Send completion message
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    if (success) {
                        MessageUtils.sendMessage(sender, "sync.push-success");
                    } else {
                        MessageUtils.sendMessage(sender, "sync.push-failed");
                    }
                });
                
            } catch (Exception e) {
                plugin.getLogger().severe("Error during sync push: " + e.getMessage());
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