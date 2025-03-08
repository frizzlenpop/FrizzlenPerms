package org.frizzlenpop.frizzlenPerms.commands.admin;

import org.bukkit.command.CommandSender;
import org.frizzlenpop.frizzlenPerms.FrizzlenPerms;
import org.frizzlenpop.frizzlenPerms.commands.SubCommand;
import org.frizzlenpop.frizzlenPerms.utils.MessageUtils;

import java.util.List;
import java.util.Map;

/**
 * Command to reload the plugin configuration.
 */
public class ReloadCommand implements SubCommand {
    
    private final FrizzlenPerms plugin;
    
    /**
     * Creates a new ReloadCommand.
     *
     * @param plugin The plugin instance
     */
    public ReloadCommand(FrizzlenPerms plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public String getName() {
        return "reload";
    }
    
    @Override
    public String getDescription() {
        return "Reloads the plugin configuration.";
    }
    
    @Override
    public String getUsage() {
        return "/frizzlenperms reload";
    }
    
    @Override
    public String getPermission() {
        return "frizzlenperms.admin.reload";
    }
    
    @Override
    public int getMinArgs() {
        return 0;
    }
    
    @Override
    public List<String> getAliases() {
        return List.of("rl");
    }
    
    @Override
    public boolean execute(CommandSender sender, String[] args) {
        // Reload configuration
        long startTime = System.currentTimeMillis();
        
        try {
            // Reload config
            plugin.getConfigManager().reloadConfig();
            
            // Reload data manager
            plugin.getDataManager().clearCaches();
            
            // Reload permission manager
            plugin.getPermissionManager().updateAllPermissions();
            
            // Reload rank manager
            // No direct reload method, but clearing DataManager cache already refreshed ranks
            
            // Reload Discord manager if enabled
            if (plugin.getConfigManager().isDiscordEnabled() && plugin.getDiscordManager() != null) {
                plugin.getDiscordManager().reload();
            }
            
            // Reload sync manager if enabled
            if (plugin.getConfigManager().isSyncEnabled() && plugin.getSyncManager() != null) {
                plugin.getSyncManager().reload();
            }
            
            // Calculate time taken
            long timeTaken = System.currentTimeMillis() - startTime;
            
            // Send success message
            MessageUtils.sendMessage(sender, "admin.reload-success", Map.of("time", String.valueOf(timeTaken)));
            return true;
        } catch (Exception e) {
            plugin.getLogger().severe("Error reloading plugin: " + e.getMessage());
            e.printStackTrace();
            MessageUtils.sendMessage(sender, "admin.reload-error", Map.of("error", e.getMessage()));
            return true;
        }
    }
} 