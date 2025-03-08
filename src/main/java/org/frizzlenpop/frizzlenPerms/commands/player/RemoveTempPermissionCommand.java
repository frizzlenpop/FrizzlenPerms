package org.frizzlenpop.frizzlenPerms.commands.player;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.frizzlenpop.frizzlenPerms.FrizzlenPerms;
import org.frizzlenpop.frizzlenPerms.commands.SubCommand;
import org.frizzlenpop.frizzlenPerms.models.AuditLog;
import org.frizzlenpop.frizzlenPerms.models.PlayerData;
import org.frizzlenpop.frizzlenPerms.utils.MessageUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
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
        return "Remove a temporary permission from a player";
    }
    
    @Override
    public String getUsage() {
        return "/perms removetemppermission <player> <permission>";
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
        final UUID playerUUID;
        final Player targetPlayer = Bukkit.getPlayer(playerName);
        
        if (targetPlayer != null) {
            playerUUID = targetPlayer.getUniqueId();
        } else {
            // Try to get UUID from offline player
            PlayerData playerData = plugin.getDataManager().getPlayerDataByName(playerName);
            if (playerData == null) {
                MessageUtils.sendMessage(sender, "error.player-not-found", Map.of(
                    "player", playerName
                ));
                return false;
            }
            playerUUID = playerData.getUuid();
        }
        
        // Remove temp permission asynchronously
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                // Check if player has the temp permission
                PlayerData playerData = plugin.getDataManager().getPlayerData(playerUUID);
                if (playerData == null) {
                    plugin.getServer().getScheduler().runTask(plugin, () -> {
                        MessageUtils.sendMessage(sender, "error.player-data-not-found", Map.of(
                            "player", playerName
                        ));
                    });
                    return;
                }
                
                // Check if player has the temp permission
                if (!playerData.isTemporaryPermission(permission)) {
                    plugin.getServer().getScheduler().runTask(plugin, () -> {
                        MessageUtils.sendMessage(sender, "error.player-no-temp-permission", Map.of(
                            "player", playerName,
                            "permission", permission
                        ));
                    });
                    return;
                }
                
                // Remove temp permission
                playerData.removeTemporaryPermission(permission);
                plugin.getDataManager().savePlayerData(playerData);
                
                // Log action
                String executorName = sender instanceof Player ? ((Player) sender).getName() : "CONSOLE";
                plugin.getAuditManager().logAction(
                    sender instanceof Player ? ((Player) sender).getUniqueId() : null,
                    executorName,
                    AuditLog.ActionType.PLAYER_TEMP_PERMISSION_REMOVE,
                    playerName,
                    "Removed temporary permission " + permission,
                    plugin.getConfigManager().getServerName(),
                    playerUUID
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
            // Try to get player's temporary permissions
            String playerName = args[0];
            Player targetPlayer = Bukkit.getPlayer(playerName);
            String partial = args[1].toLowerCase();
            
            if (targetPlayer != null) {
                PlayerData playerData = plugin.getDataManager().getPlayerData(targetPlayer.getUniqueId());
                if (playerData != null) {
                    return playerData.getTemporaryPermissions().keySet().stream()
                        .filter(perm -> perm.toLowerCase().startsWith(partial))
                        .collect(Collectors.toList());
                }
            }
        }
        
        return new ArrayList<>();
    }
} 