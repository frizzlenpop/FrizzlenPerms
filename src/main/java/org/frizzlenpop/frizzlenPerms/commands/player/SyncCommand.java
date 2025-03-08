package org.frizzlenpop.frizzlenPerms.commands.player;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.frizzlenpop.frizzlenPerms.FrizzlenPerms;
import org.frizzlenpop.frizzlenPerms.commands.SubCommand;
import org.frizzlenpop.frizzlenPerms.sync.SyncManager;
import org.frizzlenpop.frizzlenPerms.utils.MessageUtils;

import java.util.List;
import java.util.Map;

/**
 * Command to synchronize player permissions across servers.
 */
public class SyncCommand implements SubCommand {
    
    private final FrizzlenPerms plugin;
    
    /**
     * Creates a new SyncCommand.
     *
     * @param plugin The plugin instance
     */
    public SyncCommand(FrizzlenPerms plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public String getName() {
        return "sync";
    }
    
    @Override
    public String getDescription() {
        return "Synchronizes your permissions across servers.";
    }
    
    @Override
    public String getUsage() {
        return "/frizzlenperms sync";
    }
    
    @Override
    public String getPermission() {
        return "frizzlenperms.player.sync";
    }
    
    @Override
    public int getMinArgs() {
        return 0;
    }
    
    @Override
    public List<String> getAliases() {
        return List.of("synchronize", "update");
    }
    
    @Override
    public boolean isPlayerOnly() {
        return true;
    }
    
    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            MessageUtils.sendMessage(sender, "error.player-only");
            return false;
        }
        
        Player player = (Player) sender;
        
        // Check if sync is enabled
        if (!plugin.getConfig().getBoolean("sync.enabled", false)) {
            MessageUtils.sendMessage(player, "player.sync-disabled");
            return false;
        }
        
        // Get the sync manager
        SyncManager syncManager = plugin.getSyncManager();
        if (syncManager == null) {
            MessageUtils.sendMessage(player, "player.sync-error");
            return false;
        }
        
        // Send sync starting message
        MessageUtils.sendMessage(player, "player.sync-starting");
        
        // Run sync process async
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                // Perform sync operation
                boolean success = syncManager.syncPlayerWithDiscord(player);
                
                // Send result message
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    if (success) {
                        MessageUtils.sendMessage(player, "player.sync-success");
                    } else {
                        MessageUtils.sendMessage(player, "player.sync-failed");
                    }
                });
            } catch (Exception e) {
                plugin.getLogger().severe("Error syncing player " + player.getName() + ": " + e.getMessage());
                e.printStackTrace();
                
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    MessageUtils.sendMessage(player, "player.sync-error", Map.of(
                        "error", e.getMessage()
                    ));
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