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
 * Command to remove inheritance from a rank.
 */
public class RankRemoveInheritanceCommand implements SubCommand {
    
    private final FrizzlenPerms plugin;
    
    /**
     * Creates a new RankRemoveInheritanceCommand.
     *
     * @param plugin The plugin instance
     */
    public RankRemoveInheritanceCommand(FrizzlenPerms plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public String getName() {
        return "rankremoveinheritance";
    }
    
    @Override
    public String getDescription() {
        return "Removes inheritance from a rank.";
    }
    
    @Override
    public String getUsage() {
        return "/frizzlenperms rankremoveinheritance <rank> <parent>";
    }
    
    @Override
    public String getPermission() {
        return "frizzlenperms.admin.rankremoveinheritance";
    }
    
    @Override
    public int getMinArgs() {
        return 2;
    }
    
    @Override
    public List<String> getAliases() {
        return List.of("removeinheritance", "rankuninherit");
    }
    
    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (args.length < 2) {
            MessageUtils.sendMessage(sender, "error.not-enough-args", Map.of(
                "usage", getUsage()
            ));
            return false;
        }
        
        final String rankName = args[0];
        final String parentName = args[1];
        
        // Check if rank exists
        Rank rank = plugin.getRankManager().getRank(rankName);
        if (rank == null) {
            MessageUtils.sendMessage(sender, "error.rank-not-found", Map.of(
                "rank", rankName
            ));
            return false;
        }
        
        // Check if parent rank exists
        Rank parent = plugin.getRankManager().getRank(parentName);
        if (parent == null) {
            MessageUtils.sendMessage(sender, "error.rank-not-found", Map.of(
                "rank", parentName
            ));
            return false;
        }
        
        // Check if inheritance exists
        if (!rank.getInheritance().contains(parentName)) {
            MessageUtils.sendMessage(sender, "error.rank-not-inherits", Map.of(
                "rank", rankName,
                "parent", parentName
            ));
            return false;
        }
        
        // Remove inheritance asynchronously
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                // Remove inheritance
                rank.removeInheritance(parentName);
                plugin.getRankManager().removeRankPermission(rankName, "inherit." + parentName, sender instanceof Player ? (Player) sender : null);
                
                // Log to audit log
                if (sender instanceof Player) {
                    Player player = (Player) sender;
                    plugin.getAuditManager().logAction(
                        player.getUniqueId(),
                        player.getName(),
                        AuditLog.ActionType.RANK_MODIFY,
                        rankName,
                        "Removed inheritance from " + parentName,
                        plugin.getConfigManager().getServerName()
                    );
                } else {
                    plugin.getAuditManager().logAction(
                        null,
                        "Console",
                        AuditLog.ActionType.RANK_MODIFY,
                        rankName,
                        "Removed inheritance from " + parentName,
                        plugin.getConfigManager().getServerName()
                    );
                }
                
                // Send success message
                MessageUtils.sendMessage(sender, "admin.rank-remove-inheritance-success", Map.of(
                    "rank", rankName,
                    "parent", parentName
                ));
                
                // Update permissions for all online players with this rank
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    plugin.getServer().getOnlinePlayers().stream()
                        .filter(player -> plugin.getPermissionManager().hasRank(player, rank.getName()))
                        .forEach(player -> plugin.getPermissionManager().calculateAndApplyPermissions(player));
                });
                
            } catch (Exception e) {
                plugin.getLogger().severe("Error removing rank inheritance: " + e.getMessage());
                e.printStackTrace();
                MessageUtils.sendMessage(sender, "error.database-error");
            }
        });
        
        return true;
    }
    
    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            // Suggest rank names
            List<String> rankNames = plugin.getRankManager().getAllRanks().stream()
                .map(Rank::getName)
                .collect(Collectors.toList());
            
            String partial = args[0].toLowerCase();
            return rankNames.stream()
                .filter(name -> name.toLowerCase().startsWith(partial))
                .collect(Collectors.toList());
        } else if (args.length == 2) {
            // Suggest parent rank names that the rank inherits from
            String rankName = args[0];
            Rank rank = plugin.getRankManager().getRank(rankName);
            
            if (rank != null && !rank.getInheritance().isEmpty()) {
                List<String> inheritances = new ArrayList<>(rank.getInheritance());
                
                String partial = args[1].toLowerCase();
                return inheritances.stream()
                    .filter(name -> name.toLowerCase().startsWith(partial))
                    .collect(Collectors.toList());
            }
        }
        
        return Collections.emptyList();
    }
} 