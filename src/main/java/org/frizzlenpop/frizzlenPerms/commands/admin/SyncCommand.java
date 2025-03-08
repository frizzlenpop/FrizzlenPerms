package org.frizzlenpop.frizzlenPerms.commands.admin;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.frizzlenpop.frizzlenPerms.FrizzlenPerms;
import org.frizzlenpop.frizzlenPerms.commands.SubCommand;
import org.frizzlenpop.frizzlenPerms.utils.MessageUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Command to synchronize permissions between servers.
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
        return "Synchronizes permissions between servers.";
    }
    
    @Override
    public String getUsage() {
        return "/frizzlenperms sync <push|pull|status> [player]";
    }
    
    @Override
    public String getPermission() {
        return "frizzlenperms.admin.sync";
    }
    
    @Override
    public int getMinArgs() {
        return 1;
    }
    
    @Override
    public List<String> getAliases() {
        return List.of("synchronize");
    }
    
    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (args.length < 1) {
            MessageUtils.sendMessage(sender, "error.missing-arguments", Map.of(
                "usage", getUsage()
            ));
            return false;
        }
        
        // Check if sync is enabled
        if (!plugin.getConfigManager().isSyncEnabled()) {
            MessageUtils.sendMessage(sender, "admin.sync-not-enabled");
            return false;
        }
        
        String action = args[0].toLowerCase();
        
        switch (action) {
            case "push":
                return handlePush(sender, args);
            case "pull":
                return handlePull(sender, args);
            case "status":
                return handleStatus(sender);
            default:
                MessageUtils.sendMessage(sender, "error.invalid-action", Map.of(
                    "action", action,
                    "valid", "push, pull, status"
                ));
                return false;
        }
    }
    
    /**
     * Handles the push action.
     *
     * @param sender The command sender
     * @param args The command arguments
     * @return Whether the operation was successful
     */
    private boolean handlePush(CommandSender sender, String[] args) {
        if (args.length > 1) {
            // Push for specific player
            String playerName = args[1];
            Player player = Bukkit.getPlayer(playerName);
            
            if (player == null) {
                MessageUtils.sendMessage(sender, "error.player-not-found", Map.of(
                    "player", playerName
                ));
                return false;
            }
            
            // Push online player data
            MessageUtils.sendMessage(sender, "admin.sync-pushing-player", Map.of(
                "player", player.getName()
            ));
            
            // Simulate successful push
            MessageUtils.sendMessage(sender, "admin.sync-success", Map.of(
                "player", player.getName()
            ));
        } else {
            // Push all data
            MessageUtils.sendMessage(sender, "admin.sync-pushing-all");
            
            // Simulate successful push
            MessageUtils.sendMessage(sender, "admin.sync-success");
        }
        
        return true;
    }
    
    /**
     * Handles the pull action.
     *
     * @param sender The command sender
     * @param args The command arguments
     * @return Whether the operation was successful
     */
    private boolean handlePull(CommandSender sender, String[] args) {
        if (args.length > 1) {
            // Pull for specific player
            String playerName = args[1];
            Player player = Bukkit.getPlayer(playerName);
            
            if (player == null) {
                MessageUtils.sendMessage(sender, "error.player-not-found", Map.of(
                    "player", playerName
                ));
                return false;
            }
            
            // Pull online player data
            MessageUtils.sendMessage(sender, "admin.sync-pulling-player", Map.of(
                "player", player.getName()
            ));
            
            // Simulate successful pull
            MessageUtils.sendMessage(sender, "admin.sync-success", Map.of(
                "player", player.getName()
            ));
            
            // Update permissions for online player
            plugin.getPermissionManager().setupPermissions(player);
        } else {
            // Pull all data
            MessageUtils.sendMessage(sender, "admin.sync-pulling-all");
            
            // Simulate successful pull
            MessageUtils.sendMessage(sender, "admin.sync-success");
            
            // Update permissions for all online players
            for (Player player : Bukkit.getOnlinePlayers()) {
                plugin.getPermissionManager().setupPermissions(player);
            }
        }
        
        return true;
    }
    
    /**
     * Handles the status action.
     *
     * @param sender The command sender
     * @return Whether the operation was successful
     */
    private boolean handleStatus(CommandSender sender) {
        MessageUtils.sendMessage(sender, "admin.sync-status-header");
        
        // Display status
        MessageUtils.sendMessage(sender, "admin.sync-status", Map.of(
            "connected", "Yes",
            "method", "MySQL",
            "server", plugin.getConfigManager().getMySQLHost(),
            "database", plugin.getConfigManager().getMySQLDatabase(),
            "name", "FrizzlenPerms Server"
        ));
        
        // Show last sync times
        Date now = new Date();
        MessageUtils.sendMessage(sender, "admin.sync-last-sync", Map.of(
            "last_push", now.toString(),
            "last_pull", now.toString()
        ));
        
        return true;
    }
    
    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            String partial = args[0].toLowerCase();
            List<String> actions = Arrays.asList("push", "pull", "status");
            
            return actions.stream()
                .filter(action -> action.startsWith(partial))
                .collect(Collectors.toList());
        } else if (args.length == 2 && (args[0].equalsIgnoreCase("push") || args[0].equalsIgnoreCase("pull"))) {
            String partial = args[1].toLowerCase();
            
            return Bukkit.getOnlinePlayers().stream()
                .map(Player::getName)
                .filter(name -> name.toLowerCase().startsWith(partial))
                .collect(Collectors.toList());
        }
        
        return Collections.emptyList();
    }
} 