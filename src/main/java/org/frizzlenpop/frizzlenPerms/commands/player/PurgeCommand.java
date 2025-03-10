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
 * Command to purge a player's data from the database.
 */
public class PurgeCommand implements SubCommand {
    
    private final FrizzlenPerms plugin;
    
    /**
     * Creates a new PurgeCommand.
     *
     * @param plugin The plugin instance
     */
    public PurgeCommand(FrizzlenPerms plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public String getName() {
        return "purge";
    }
    
    @Override
    public String getDescription() {
        return "Purges a player's data from the database.";
    }
    
    @Override
    public String getUsage() {
        return "/frizzlenperms purge <player> [confirm]";
    }
    
    @Override
    public String getPermission() {
        return "frizzlenperms.admin.purge";
    }
    
    @Override
    public int getMinArgs() {
        return 1;
    }
    
    @Override
    public List<String> getAliases() {
        return List.of("delete", "remove");
    }
    
    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (args.length < 1) {
            MessageUtils.sendMessage(sender, "error.missing-arguments", Map.of(
                "usage", getUsage()
            ));
            return false;
        }
        
        String playerName = args[0];
        boolean confirmed = args.length > 1 && args[1].equalsIgnoreCase("confirm");
        
        // Get player UUID
        UUID playerUUID = null;
        Player targetPlayer = Bukkit.getPlayer(playerName);
        
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
        
        // Require confirmation
        if (!confirmed) {
            MessageUtils.sendMessage(sender, "admin.purge-confirm", Map.of(
                "player", playerName,
                "command", "/frizzlenperms purge " + playerName + " confirm"
            ));
            return true;
        }
        
        // Store UUID in final variable for async use
        final UUID finalPlayerUUID = playerUUID;
        
        // Purge player data asynchronously
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                // Remove player from any ranks they have
                PlayerData playerData = plugin.getDataManager().getPlayerData(finalPlayerUUID);
                if (playerData != null) {
                    // Clear ranks
                    playerData.setPrimaryRank(null);
                    playerData.getSecondaryRanks().clear();
                    playerData.getTemporaryRanks().clear();
                    
                    // Clear permissions
                    playerData.getPermissions().clear();
                    playerData.getTemporaryPermissions().clear();
                    playerData.getWorldPermissions().clear();
                    
                    // Save cleared data before purging
                    plugin.getDataManager().savePlayerData(playerData);
                    
                    // Update permissions if player is online
                    if (targetPlayer != null && targetPlayer.isOnline()) {
                        plugin.getServer().getScheduler().runTask(plugin, () -> {
                            plugin.getPermissionManager().calculateAndApplyPermissions(targetPlayer);
                        });
                    }
                }
                
                // Purge player data
                plugin.getDataManager().deletePlayerData(finalPlayerUUID);
                
                // Log action
                plugin.getAuditManager().logAction(
                    sender instanceof Player ? ((Player) sender).getUniqueId() : null,
                    sender instanceof Player ? ((Player) sender).getName() : "CONSOLE",
                    AuditLog.ActionType.PLAYER_DATA_PURGE,
                    playerName,
                    "Purged player data",
                    plugin.getConfigManager().getServerName(),
                    finalPlayerUUID
                );
                
                // Kick player if online
                if (targetPlayer != null && targetPlayer.isOnline()) {
                    plugin.getServer().getScheduler().runTask(plugin, () -> {
                        targetPlayer.kickPlayer(MessageUtils.formatColors("&cYour permissions data has been purged."));
                    });
                }
                
                // Send success message
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    MessageUtils.sendMessage(sender, "admin.purge-success", Map.of(
                        "player", playerName
                    ));
                });
            } catch (Exception e) {
                plugin.getLogger().severe("Error purging player data: " + e.getMessage());
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
            // Suggest confirm
            return List.of("confirm");
        }
        
        return Collections.emptyList();
    }
} 