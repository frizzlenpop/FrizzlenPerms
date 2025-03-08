package org.frizzlenpop.frizzlenPerms.commands.player;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.frizzlenpop.frizzlenPerms.FrizzlenPerms;
import org.frizzlenpop.frizzlenPerms.commands.SubCommand;
import org.frizzlenpop.frizzlenPerms.models.AuditLog;
import org.frizzlenpop.frizzlenPerms.models.PlayerData;
import org.frizzlenpop.frizzlenPerms.utils.MessageUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Command to remove a temporary permission from a player.
 */
public class RemoveTempPermissionCommand implements SubCommand {
    
    private final FrizzlenPerms plugin;
    
    /**
     * Creates a new RemoveTempPermissionCommand.
     *
     * @param plugin The plugin instance
     */
    public RemoveTempPermissionCommand(FrizzlenPerms plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public String getName() {
        return "removetemppermission";
    }
    
    @Override
    public String getDescription() {
        return "Removes a temporary permission from a player.";
    }
    
    @Override
    public String getUsage() {
        return "/frizzlenperms removetemppermission <player> <permission>";
    }
    
    @Override
    public String getPermission() {
        return "frizzlenperms.admin.removetemppermission";
    }
    
    @Override
    public int getMinArgs() {
        return 2;
    }
    
    @Override
    public List<String> getAliases() {
        return List.of("temppermremove", "deltempperm", "removetempperm");
    }
    
    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (args.length < 2) {
            MessageUtils.sendMessage(sender, "error.missing-arguments", Map.of(
                "usage", getUsage()
            ));
            return false;
        }
        
        String playerName = args[0];
        String permission = args[1];
        
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
        
        // Store UUID in final variable for async use
        final UUID finalPlayerUUID = playerUUID;
        
        // Remove temp permission asynchronously
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                // Check if player has the temp permission
                PlayerData playerData = plugin.getDataManager().getPlayerData(finalPlayerUUID);
                if (playerData == null) {
                    plugin.getServer().getScheduler().runTask(plugin, () -> {
                        MessageUtils.sendMessage(sender, "error.player-data-not-found", Map.of(
                            "player", playerName
                        ));
                    });
                    return;
                }
                
                // Check if player has the temp permission
                boolean hasTempPermission = playerData.hasTempPermission(permission);
                if (!hasTempPermission) {
                    plugin.getServer().getScheduler().runTask(plugin, () -> {
                        MessageUtils.sendMessage(sender, "error.player-no-temp-permission", Map.of(
                            "player", playerName,
                            "permission", permission
                        ));
                    });
                    return;
                }
                
                // Remove temp permission
                boolean success = plugin.getDataManager().removeTempPermission(finalPlayerUUID, permission);
                
                if (success) {
                    // Log action
                    String executorName = sender instanceof Player ? ((Player) sender).getName() : "CONSOLE";
                    plugin.getAuditManager().logAction(
                        AuditLog.ActionType.PLAYER_TEMP_PERMISSION_REMOVE,
                        finalPlayerUUID,
                        sender instanceof Player ? ((Player) sender).getUniqueId() : null,
                        "Removed temporary permission " + permission,
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
                        MessageUtils.sendMessage(sender, "admin.temp-permission-removed", Map.of(
                            "player", playerName,
                            "permission", permission
                        ));
                    });
                } else {
                    plugin.getServer().getScheduler().runTask(plugin, () -> {
                        MessageUtils.sendMessage(sender, "error.temp-permission-remove-failed", Map.of(
                            "player", playerName,
                            "permission", permission
                        ));
                    });
                }
            } catch (Exception e) {
                plugin.getLogger().severe("Error removing temporary permission: " + e.getMessage());
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
            // Suggest player's temp permissions
            String partial = args[1].toLowerCase();
            
            // Try to get player's temp permissions
            Player targetPlayer = Bukkit.getPlayer(args[0]);
            if (targetPlayer != null) {
                UUID playerUUID = targetPlayer.getUniqueId();
                PlayerData playerData = plugin.getDataManager().getPlayerData(playerUUID);
                
                if (playerData != null) {
                    return playerData.getTempPermissions().stream()
                        .map(tempPerm -> tempPerm.getPermission())
                        .filter(perm -> perm.toLowerCase().startsWith(partial))
                        .collect(Collectors.toList());
                }
            }
            
            // Fallback to common permissions
            List<String> commonPerms = Arrays.asList(
                "minecraft.command.",
                "bukkit.command.",
                "frizzlenperms."
            );
            
            return commonPerms.stream()
                .filter(perm -> perm.toLowerCase().startsWith(partial))
                .collect(Collectors.toList());
        }
        
        return Collections.emptyList();
    }
} 