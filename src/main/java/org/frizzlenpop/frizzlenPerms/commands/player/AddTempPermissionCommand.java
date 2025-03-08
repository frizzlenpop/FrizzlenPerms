package org.frizzlenpop.frizzlenPerms.commands.player;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.frizzlenpop.frizzlenPerms.FrizzlenPerms;
import org.frizzlenpop.frizzlenPerms.commands.SubCommand;
import org.frizzlenpop.frizzlenPerms.models.AuditLog;
import org.frizzlenpop.frizzlenPerms.models.TempPermission;
import org.frizzlenpop.frizzlenPerms.utils.MessageUtils;
import org.frizzlenpop.frizzlenPerms.utils.TimeUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Command to add a temporary permission to a player.
 */
public class AddTempPermissionCommand implements SubCommand {
    
    private final FrizzlenPerms plugin;
    
    /**
     * Creates a new AddTempPermissionCommand.
     *
     * @param plugin The plugin instance
     */
    public AddTempPermissionCommand(FrizzlenPerms plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public String getName() {
        return "addtemppermission";
    }
    
    @Override
    public String getDescription() {
        return "Adds a temporary permission to a player.";
    }
    
    @Override
    public String getUsage() {
        return "/frizzlenperms addtemppermission <player> <permission> <duration>";
    }
    
    @Override
    public String getPermission() {
        return "frizzlenperms.admin.addtemppermission";
    }
    
    @Override
    public int getMinArgs() {
        return 3;
    }
    
    @Override
    public List<String> getAliases() {
        return List.of("temppermgive", "givetempperm", "tempperm");
    }
    
    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (args.length < 3) {
            MessageUtils.sendMessage(sender, "error.missing-arguments", Map.of(
                "usage", getUsage()
            ));
            return false;
        }
        
        String playerName = args[0];
        String permission = args[1];
        String durationStr = args[2];
        
        // Parse duration
        long duration;
        try {
            duration = TimeUtils.parseDuration(durationStr);
            if (duration <= 0) {
                MessageUtils.sendMessage(sender, "error.invalid-duration");
                return false;
            }
        } catch (IllegalArgumentException e) {
            MessageUtils.sendMessage(sender, "error.invalid-duration-format");
            return false;
        }
        
        // Get player data
        UUID playerUUID = null;
        Player targetPlayer = Bukkit.getPlayer(playerName);
        
        if (targetPlayer != null) {
            playerUUID = targetPlayer.getUniqueId();
        } else {
            // Try to get UUID from offline player
            playerUUID = plugin.getDataManager().getPlayerUUID(playerName);
            if (playerUUID == null) {
                MessageUtils.sendMessage(sender, "error.player-not-found", Map.of(
                    "player", playerName
                ));
                return false;
            }
        }
        
        // Calculate expiration time
        long expirationTime = System.currentTimeMillis() + duration;
        
        // Create temp permission
        TempPermission tempPermission = new TempPermission(permission, expirationTime);
        
        // Add temp permission asynchronously
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                // Add temp permission
                boolean success = plugin.getDataManager().addTempPermission(playerUUID, tempPermission);
                
                if (success) {
                    // Log action
                    String executorName = sender instanceof Player ? ((Player) sender).getName() : "CONSOLE";
                    plugin.getAuditManager().logAction(
                        AuditLog.ActionType.PLAYER_TEMP_PERMISSION_ADD,
                        playerUUID,
                        sender instanceof Player ? ((Player) sender).getUniqueId() : null,
                        "Added temporary permission " + permission + " for " + TimeUtils.formatDuration(duration),
                        plugin.getConfigManager().getServerName()
                    );
                    
                    // Apply changes if player is online
                    if (targetPlayer != null) {
                        plugin.getServer().getScheduler().runTask(plugin, () -> {
                            plugin.getPermissionManager().calculateAndApplyPermissions(targetPlayer);
                        });
                    }
                    
                    // Send success message
                    plugin.getServer().getScheduler().runTask(plugin, () -> {
                        MessageUtils.sendMessage(sender, "admin.temp-permission-added", Map.of(
                            "player", playerName,
                            "permission", permission,
                            "duration", TimeUtils.formatDuration(duration)
                        ));
                    });
                } else {
                    plugin.getServer().getScheduler().runTask(plugin, () -> {
                        MessageUtils.sendMessage(sender, "error.temp-permission-add-failed", Map.of(
                            "player", playerName,
                            "permission", permission
                        ));
                    });
                }
            } catch (Exception e) {
                plugin.getLogger().severe("Error adding temporary permission: " + e.getMessage());
                e.printStackTrace();
                
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    MessageUtils.sendMessage(sender, "error.internal-error", Map.of(
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
            // Suggest player names
            String partial = args[0].toLowerCase();
            return Bukkit.getOnlinePlayers().stream()
                .map(Player::getName)
                .filter(name -> name.toLowerCase().startsWith(partial))
                .collect(Collectors.toList());
        } else if (args.length == 2) {
            // Suggest common permissions
            String partial = args[1].toLowerCase();
            List<String> commonPerms = Arrays.asList(
                "minecraft.command.",
                "bukkit.command.",
                "frizzlenperms."
            );
            
            return commonPerms.stream()
                .filter(perm -> perm.toLowerCase().startsWith(partial))
                .collect(Collectors.toList());
        } else if (args.length == 3) {
            // Suggest durations
            return List.of("1h", "1d", "7d", "30d");
        }
        
        return Collections.emptyList();
    }
} 