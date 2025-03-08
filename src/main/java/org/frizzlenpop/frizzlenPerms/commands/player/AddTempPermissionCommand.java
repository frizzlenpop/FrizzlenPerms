package org.frizzlenpop.frizzlenPerms.commands.player;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.frizzlenpop.frizzlenPerms.FrizzlenPerms;
import org.frizzlenpop.frizzlenPerms.commands.SubCommand;
import org.frizzlenpop.frizzlenPerms.models.AuditLog;
import org.frizzlenpop.frizzlenPerms.models.PlayerData;
import org.frizzlenpop.frizzlenPerms.models.TempPermission;
import org.frizzlenpop.frizzlenPerms.utils.MessageUtils;
import org.frizzlenpop.frizzlenPerms.utils.TimeUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
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
        return "Add a temporary permission to a player";
    }
    
    @Override
    public String getUsage() {
        return "/perms addtemppermission <player> <permission> <duration>";
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
        
        // Calculate expiration time
        long expirationTime = System.currentTimeMillis() + duration;
        
        // Create temp permission
        TempPermission tempPermission = new TempPermission(permission, expirationTime);
        
        // Add temp permission asynchronously
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                // Get player data
                PlayerData playerData = plugin.getDataManager().getPlayerData(playerUUID);
                if (playerData == null) {
                    plugin.getServer().getScheduler().runTask(plugin, () -> {
                        MessageUtils.sendMessage(sender, "error.player-data-not-found", Map.of(
                            "player", playerName
                        ));
                    });
                    return;
                }
                
                // Add the temp permission
                playerData.addTemporaryPermission(tempPermission.getPermission(), tempPermission.getExpirationTime());
                plugin.getDataManager().savePlayerData(playerData);
                
                // Log action
                String executorName = sender instanceof Player ? ((Player) sender).getName() : "CONSOLE";
                plugin.getAuditManager().logAction(
                    sender instanceof Player ? ((Player) sender).getUniqueId() : null,
                    executorName,
                    AuditLog.ActionType.PLAYER_TEMP_PERMISSION_ADD,
                    playerName,
                    "Added temporary permission " + permission + " for " + TimeUtils.formatDuration(duration),
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
                    MessageUtils.sendMessage(sender, "admin.temp-permission-added", Map.of(
                        "player", playerName,
                        "permission", permission,
                        "duration", TimeUtils.formatDuration(duration)
                    ));
                });
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
            return List.of(
                "minecraft.command.gamemode",
                "minecraft.command.tp",
                "minecraft.command.give",
                "minecraft.command.kick",
                "minecraft.command.ban",
                "minecraft.command.op",
                "minecraft.command.deop"
            ).stream()
                .filter(perm -> perm.toLowerCase().startsWith(partial))
                .collect(Collectors.toList());
        } else if (args.length == 3) {
            // Suggest durations
            return List.of("1h", "1d", "7d", "30d");
        }
        
        return new ArrayList<>();
    }
} 