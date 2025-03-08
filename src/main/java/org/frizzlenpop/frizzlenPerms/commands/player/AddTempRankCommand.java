package org.frizzlenpop.frizzlenPerms.commands.player;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.frizzlenpop.frizzlenPerms.FrizzlenPerms;
import org.frizzlenpop.frizzlenPerms.commands.SubCommand;
import org.frizzlenpop.frizzlenPerms.models.AuditLog;
import org.frizzlenpop.frizzlenPerms.models.Rank;
import org.frizzlenpop.frizzlenPerms.models.TempRank;
import org.frizzlenpop.frizzlenPerms.utils.MessageUtils;
import org.frizzlenpop.frizzlenPerms.utils.TimeUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Command to add a temporary rank to a player.
 */
public class AddTempRankCommand implements SubCommand {
    
    private final FrizzlenPerms plugin;
    
    /**
     * Creates a new AddTempRankCommand.
     *
     * @param plugin The plugin instance
     */
    public AddTempRankCommand(FrizzlenPerms plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public String getName() {
        return "addtemprank";
    }
    
    @Override
    public String getDescription() {
        return "Adds a temporary rank to a player.";
    }
    
    @Override
    public String getUsage() {
        return "/frizzlenperms addtemprank <player> <rank> <duration>";
    }
    
    @Override
    public String getPermission() {
        return "frizzlenperms.admin.addtemprank";
    }
    
    @Override
    public int getMinArgs() {
        return 3;
    }
    
    @Override
    public List<String> getAliases() {
        return List.of("temprankgive", "givetemprank", "temprank");
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
        String rankName = args[1];
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
        
        // Calculate expiration time
        long expirationTime = System.currentTimeMillis() + duration;
        
        // Create temp rank
        TempRank tempRank = new TempRank(rankName, expirationTime);
        
        // Add temp rank asynchronously
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                // Add temp rank
                boolean success = plugin.getDataManager().addTempRank(playerUUID, tempRank);
                
                if (success) {
                    // Log action
                    String executorName = sender instanceof Player ? ((Player) sender).getName() : "CONSOLE";
                    plugin.getAuditManager().logAction(
                        AuditLog.ActionType.PLAYER_TEMP_RANK_ADD,
                        playerUUID,
                        sender instanceof Player ? ((Player) sender).getUniqueId() : null,
                        "Added temporary rank " + rankName + " for " + TimeUtils.formatDuration(duration),
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
                        MessageUtils.sendMessage(sender, "admin.temp-rank-added", Map.of(
                            "player", playerName,
                            "rank", rank.getDisplayName(),
                            "duration", TimeUtils.formatDuration(duration)
                        ));
                    });
                } else {
                    plugin.getServer().getScheduler().runTask(plugin, () -> {
                        MessageUtils.sendMessage(sender, "error.temp-rank-add-failed", Map.of(
                            "player", playerName,
                            "rank", rankName
                        ));
                    });
                }
            } catch (Exception e) {
                plugin.getLogger().severe("Error adding temporary rank: " + e.getMessage());
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
            // Suggest rank names
            String partial = args[1].toLowerCase();
            return plugin.getRankManager().getRanks().stream()
                .map(Rank::getName)
                .filter(name -> name.toLowerCase().startsWith(partial))
                .collect(Collectors.toList());
        } else if (args.length == 3) {
            // Suggest durations
            return List.of("1h", "1d", "7d", "30d");
        }
        
        return Collections.emptyList();
    }
} 