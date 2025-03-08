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
 * Command to clone a player's permissions and ranks to another player.
 */
public class CloneCommand implements SubCommand {
    
    private final FrizzlenPerms plugin;
    
    /**
     * Creates a new CloneCommand.
     *
     * @param plugin The plugin instance
     */
    public CloneCommand(FrizzlenPerms plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public String getName() {
        return "clone";
    }
    
    @Override
    public String getDescription() {
        return "Clones a player's permissions and ranks to another player.";
    }
    
    @Override
    public String getUsage() {
        return "/frizzlenperms clone <source> <target>";
    }
    
    @Override
    public String getPermission() {
        return "frizzlenperms.admin.clone";
    }
    
    @Override
    public int getMinArgs() {
        return 2;
    }
    
    @Override
    public List<String> getAliases() {
        return List.of("copy");
    }
    
    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (args.length < 2) {
            MessageUtils.sendMessage(sender, "error.missing-arguments", Map.of(
                "usage", getUsage()
            ));
            return false;
        }
        
        String sourcePlayerName = args[0];
        String targetPlayerName = args[1];
        
        // Check if source and target are the same
        if (sourcePlayerName.equalsIgnoreCase(targetPlayerName)) {
            MessageUtils.sendMessage(sender, "error.clone-same-player");
            return false;
        }
        
        // Get source player UUID
        UUID sourcePlayerUUID = null;
        Player sourcePlayer = Bukkit.getPlayer(sourcePlayerName);
        
        if (sourcePlayer != null) {
            sourcePlayerUUID = sourcePlayer.getUniqueId();
        } else {
            // Try to get UUID from offline player
            PlayerData sourceData = plugin.getDataManager().getPlayerDataByName(sourcePlayerName);
            if (sourceData == null) {
                MessageUtils.sendMessage(sender, "error.player-not-found", Map.of(
                    "player", sourcePlayerName
                ));
                return false;
            }
            sourcePlayerUUID = sourceData.getUuid();
        }
        
        // Get target player UUID
        UUID targetPlayerUUID = null;
        Player targetPlayer = Bukkit.getPlayer(targetPlayerName);
        
        if (targetPlayer != null) {
            targetPlayerUUID = targetPlayer.getUniqueId();
        } else {
            // Try to get UUID from offline player
            PlayerData targetData = plugin.getDataManager().getPlayerDataByName(targetPlayerName);
            if (targetData == null) {
                MessageUtils.sendMessage(sender, "error.player-not-found", Map.of(
                    "player", targetPlayerName
                ));
                return false;
            }
            targetPlayerUUID = targetData.getUuid();
        }
        
        // Store UUIDs in final variables for async use
        final UUID finalSourcePlayerUUID = sourcePlayerUUID;
        final UUID finalTargetPlayerUUID = targetPlayerUUID;
        
        // Clone player data asynchronously
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                // Get source player data
                PlayerData sourcePlayerData = plugin.getDataManager().getPlayerData(finalSourcePlayerUUID);
                if (sourcePlayerData == null) {
                    plugin.getServer().getScheduler().runTask(plugin, () -> {
                        MessageUtils.sendMessage(sender, "error.player-data-not-found", Map.of(
                            "player", sourcePlayerName
                        ));
                    });
                    return;
                }
                
                // Get target player data
                PlayerData targetPlayerData = plugin.getDataManager().getPlayerData(finalTargetPlayerUUID);
                if (targetPlayerData == null) {
                    // Create new player data for target
                    targetPlayerData = new PlayerData(finalTargetPlayerUUID, targetPlayerName);
                }
                
                // Clone data
                targetPlayerData.setPrimaryRank(sourcePlayerData.getPrimaryRank());
                targetPlayerData.getSecondaryRanks().clear();
                targetPlayerData.getSecondaryRanks().addAll(sourcePlayerData.getSecondaryRanks());
                targetPlayerData.getPermissions().clear();
                targetPlayerData.getPermissions().addAll(sourcePlayerData.getPermissions());
                
                // Clone temporary ranks and permissions
                targetPlayerData.getTemporaryRanks().clear();
                targetPlayerData.getTemporaryRanks().putAll(sourcePlayerData.getTemporaryRanks());
                targetPlayerData.getTemporaryPermissions().clear();
                targetPlayerData.getTemporaryPermissions().putAll(sourcePlayerData.getTemporaryPermissions());
                
                // Clone world-specific permissions
                targetPlayerData.getWorldPermissions().clear();
                targetPlayerData.getWorldPermissions().putAll(sourcePlayerData.getWorldPermissions());
                
                // Save target player data
                plugin.getDataManager().savePlayerData(targetPlayerData);
                
                // Log action
                plugin.getAuditManager().logAction(
                    sender instanceof Player ? ((Player) sender).getUniqueId() : null,
                    sender instanceof Player ? ((Player) sender).getName() : "CONSOLE",
                    AuditLog.ActionType.PLAYER_RANK_CLONE,
                    targetPlayerName,
                    "Cloned data from " + sourcePlayerName,
                    plugin.getConfigManager().getServerName(),
                    finalTargetPlayerUUID
                );
                
                // Apply changes if target player is online
                if (targetPlayer != null) {
                    plugin.getServer().getScheduler().runTask(plugin, () -> {
                        plugin.getPermissionManager().calculateAndApplyPermissions(targetPlayer);
                    });
                }
                
                // Send success message
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    MessageUtils.sendMessage(sender, "admin.clone-success", Map.of(
                        "source", sourcePlayerName,
                        "target", targetPlayerName
                    ));
                });
            } catch (Exception e) {
                plugin.getLogger().severe("Error cloning player data: " + e.getMessage());
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
        if (args.length == 1 || args.length == 2) {
            // Suggest player names
            String partial = args[args.length - 1].toLowerCase();
            return Bukkit.getOnlinePlayers().stream()
                .map(Player::getName)
                .filter(name -> name.toLowerCase().startsWith(partial))
                .collect(Collectors.toList());
        }
        
        return Collections.emptyList();
    }
} 