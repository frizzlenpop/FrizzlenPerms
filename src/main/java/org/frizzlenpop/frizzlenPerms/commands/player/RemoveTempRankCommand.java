package org.frizzlenpop.frizzlenPerms.commands.player;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.frizzlenpop.frizzlenPerms.FrizzlenPerms;
import org.frizzlenpop.frizzlenPerms.commands.SubCommand;
import org.frizzlenpop.frizzlenPerms.models.AuditLog;
import org.frizzlenpop.frizzlenPerms.models.PlayerData;
import org.frizzlenpop.frizzlenPerms.models.Rank;
import org.frizzlenpop.frizzlenPerms.utils.MessageUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Command to remove a temporary rank from a player.
 */
public class RemoveTempRankCommand implements SubCommand {
    
    private final FrizzlenPerms plugin;
    
    /**
     * Creates a new RemoveTempRankCommand.
     *
     * @param plugin The plugin instance
     */
    public RemoveTempRankCommand(FrizzlenPerms plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public String getName() {
        return "removetemprank";
    }
    
    @Override
    public String getDescription() {
        return "Removes a temporary rank from a player.";
    }
    
    @Override
    public String getUsage() {
        return "/frizzlenperms removetemprank <player> <rank>";
    }
    
    @Override
    public String getPermission() {
        return "frizzlenperms.admin.removetemprank";
    }
    
    @Override
    public int getMinArgs() {
        return 2;
    }
    
    @Override
    public List<String> getAliases() {
        return List.of("temprankremove", "deltemprank", "removetrank");
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
        String rankName = args[1];
        
        // Check if rank exists
        Rank rank = plugin.getRankManager().getRank(rankName);
        if (rank == null) {
            MessageUtils.sendMessage(sender, "error.rank-not-found", Map.of(
                "rank", rankName
            ));
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
        
        // Store UUID in final variable for async use
        final UUID finalPlayerUUID = playerUUID;
        
        // Remove temp rank asynchronously
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                // Check if player has the temp rank
                PlayerData playerData = plugin.getDataManager().getPlayerData(finalPlayerUUID);
                if (playerData == null) {
                    plugin.getServer().getScheduler().runTask(plugin, () -> {
                        MessageUtils.sendMessage(sender, "error.player-data-not-found", Map.of(
                            "player", playerName
                        ));
                    });
                    return;
                }
                
                // Check if player has the temp rank
                boolean hasTempRank = playerData.hasTempRank(rankName);
                if (!hasTempRank) {
                    plugin.getServer().getScheduler().runTask(plugin, () -> {
                        MessageUtils.sendMessage(sender, "error.player-no-temp-rank", Map.of(
                            "player", playerName,
                            "rank", rankName
                        ));
                    });
                    return;
                }
                
                // Remove temp rank
                boolean success = plugin.getDataManager().removeTempRank(finalPlayerUUID, rankName);
                
                if (success) {
                    // Log action
                    String executorName = sender instanceof Player ? ((Player) sender).getName() : "CONSOLE";
                    plugin.getAuditManager().logAction(
                        AuditLog.ActionType.PLAYER_TEMP_RANK_REMOVE,
                        finalPlayerUUID,
                        sender instanceof Player ? ((Player) sender).getUniqueId() : null,
                        "Removed temporary rank " + rankName,
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
                        MessageUtils.sendMessage(sender, "admin.temp-rank-removed", Map.of(
                            "player", playerName,
                            "rank", rank.getDisplayName()
                        ));
                    });
                } else {
                    plugin.getServer().getScheduler().runTask(plugin, () -> {
                        MessageUtils.sendMessage(sender, "error.temp-rank-remove-failed", Map.of(
                            "player", playerName,
                            "rank", rankName
                        ));
                    });
                }
            } catch (Exception e) {
                plugin.getLogger().severe("Error removing temporary rank: " + e.getMessage());
                e.printStackTrace();
                
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    MessageUtils.sendMessage(sender, "error.remove-rank-failed", Map.of(
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
            // Suggest rank names
            String partial = args[1].toLowerCase();
            
            // Try to get player's temp ranks
            Player targetPlayer = Bukkit.getPlayer(args[0]);
            if (targetPlayer != null) {
                UUID playerUUID = targetPlayer.getUniqueId();
                PlayerData playerData = plugin.getDataManager().getPlayerData(playerUUID);
                
                if (playerData != null) {
                    return playerData.getTempRanks().stream()
                        .map(tempRank -> tempRank.getRankName())
                        .filter(name -> name.toLowerCase().startsWith(partial))
                        .collect(Collectors.toList());
                }
            }
            
            // Fallback to all ranks
            return plugin.getRankManager().getRanks().stream()
                .map(Rank::getName)
                .filter(name -> name.toLowerCase().startsWith(partial))
                .collect(Collectors.toList());
        }
        
        return Collections.emptyList();
    }
} 