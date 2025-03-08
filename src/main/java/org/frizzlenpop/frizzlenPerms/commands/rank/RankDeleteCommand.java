package org.frizzlenpop.frizzlenPerms.commands.rank;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.frizzlenpop.frizzlenPerms.FrizzlenPerms;
import org.frizzlenpop.frizzlenPerms.commands.SubCommand;
import org.frizzlenpop.frizzlenPerms.models.AuditLog;
import org.frizzlenpop.frizzlenPerms.models.Rank;
import org.frizzlenpop.frizzlenPerms.utils.MessageUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Command to delete a rank.
 */
public class RankDeleteCommand implements SubCommand {
    
    private final FrizzlenPerms plugin;
    private final Set<String> confirmations = new HashSet<>();
    
    /**
     * Creates a new RankDeleteCommand.
     *
     * @param plugin The plugin instance
     */
    public RankDeleteCommand(FrizzlenPerms plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public String getName() {
        return "rankdelete";
    }
    
    @Override
    public String getDescription() {
        return "Deletes a rank.";
    }
    
    @Override
    public String getUsage() {
        return "/frizzlenperms rankdelete <rank> [confirm]";
    }
    
    @Override
    public String getPermission() {
        return "frizzlenperms.admin.rankdelete";
    }
    
    @Override
    public int getMinArgs() {
        return 1;
    }
    
    @Override
    public List<String> getAliases() {
        return List.of("delrank", "removerank");
    }
    
    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (args.length < 1) {
            MessageUtils.sendMessage(sender, "error.not-enough-args", Map.of(
                "usage", getUsage()
            ));
            return false;
        }
        
        final String rankName = args[0];
        
        // Check if rank exists
        Rank rank = plugin.getRankManager().getRank(rankName);
        if (rank == null) {
            MessageUtils.sendMessage(sender, "error.rank-not-found", Map.of(
                "rank", rankName
            ));
            return false;
        }
        
        // Check if it's the default rank
        if (rank.isDefault()) {
            MessageUtils.sendMessage(sender, "error.cannot-delete-default-rank", Map.of(
                "rank", rankName
            ));
            return false;
        }
        
        // Check for confirmation
        boolean confirmed = args.length > 1 && args[1].equalsIgnoreCase("confirm");
        String confirmKey = (sender instanceof Player ? ((Player) sender).getUniqueId().toString() : "console") + ":" + rankName;
        
        if (!confirmed && !confirmations.contains(confirmKey)) {
            confirmations.add(confirmKey);
            
            // Schedule removal of confirmation after 60 seconds
            plugin.getServer().getScheduler().runTaskLaterAsynchronously(plugin, () -> {
                confirmations.remove(confirmKey);
            }, 1200L); // 60 seconds * 20 ticks
            
            MessageUtils.sendMessage(sender, "ranks.delete-confirm", Map.of(
                "rank", rankName,
                "command", "/frizzlenperms rankdelete " + rankName + " confirm"
            ));
            return true;
        }
        
        // Remove confirmation
        confirmations.remove(confirmKey);
        
        // Delete rank asynchronously
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                // Delete the rank
                boolean success = plugin.getRankManager().deleteRank(rankName);
                
                if (success) {
                    // Log to audit log
                    if (sender instanceof Player) {
                        Player player = (Player) sender;
                        plugin.getAuditManager().logAction(
                            player.getUniqueId(),
                            player.getName(),
                            AuditLog.ActionType.RANK_DELETE,
                            rankName,
                            "Deleted rank " + rankName,
                            plugin.getConfigManager().getServerName()
                        );
                    } else {
                        plugin.getAuditManager().logAction(
                            null,
                            "Console",
                            AuditLog.ActionType.RANK_DELETE,
                            rankName,
                            "Deleted rank " + rankName,
                            plugin.getConfigManager().getServerName()
                        );
                    }
                    
                    // Send success message
                    MessageUtils.sendMessage(sender, "ranks.delete-success", Map.of(
                        "rank", rankName
                    ));
                } else {
                    MessageUtils.sendMessage(sender, "ranks.delete-failed", Map.of(
                        "rank", rankName
                    ));
                }
                
            } catch (Exception e) {
                plugin.getLogger().severe("Error deleting rank: " + e.getMessage());
                e.printStackTrace();
                MessageUtils.sendMessage(sender, "error.database-error");
            }
        });
        
        return true;
    }
    
    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            // Suggest rank names (excluding default rank)
            List<String> rankNames = plugin.getRankManager().getRanks().stream()
                .filter(rank -> !rank.isDefault())
                .map(Rank::getName)
                .collect(Collectors.toList());
            
            String partial = args[0].toLowerCase();
            return rankNames.stream()
                .filter(name -> name.toLowerCase().startsWith(partial))
                .collect(Collectors.toList());
        } else if (args.length == 2) {
            // Suggest confirmation
            return List.of("confirm");
        }
        
        return Collections.emptyList();
    }
} 