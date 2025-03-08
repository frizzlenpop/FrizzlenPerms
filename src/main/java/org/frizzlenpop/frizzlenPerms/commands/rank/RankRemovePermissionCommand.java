package org.frizzlenpop.frizzlenPerms.commands.rank;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.frizzlenpop.frizzlenPerms.FrizzlenPerms;
import org.frizzlenpop.frizzlenPerms.commands.SubCommand;
import org.frizzlenpop.frizzlenPerms.models.Rank;
import org.frizzlenpop.frizzlenPerms.utils.MessageUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Command to remove a permission from a rank.
 */
public class RankRemovePermissionCommand implements SubCommand {
    
    private final FrizzlenPerms plugin;
    
    /**
     * Creates a new RankRemovePermissionCommand.
     *
     * @param plugin The plugin instance
     */
    public RankRemovePermissionCommand(FrizzlenPerms plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public String getName() {
        return "rankremovepermission";
    }
    
    @Override
    public String getDescription() {
        return "Removes a permission from a rank.";
    }
    
    @Override
    public String getUsage() {
        return "/frizzlenperms rankremovepermission <rank> <permission>";
    }
    
    @Override
    public String getPermission() {
        return "frizzlenperms.admin.rankremovepermission";
    }
    
    @Override
    public int getMinArgs() {
        return 2;
    }
    
    @Override
    public List<String> getAliases() {
        return List.of("removepermfromrank", "removepermissionfromrank", "rankpermissionremove");
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
        final String permission = args[1];
        
        // Check if rank exists
        Rank rank = plugin.getRankManager().getRank(rankName);
        if (rank == null) {
            MessageUtils.sendMessage(sender, "error.rank-not-found", Map.of(
                "rank", rankName
            ));
            return false;
        }
        
        // Check if permission exists for the rank
        if (!rank.hasPermission(permission)) {
            MessageUtils.sendMessage(sender, "error.rank-permission-not-found", Map.of(
                "rank", rank.getName(),
                "permission", permission
            ));
            return false;
        }
        
        // Remove permission asynchronously
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                // Remove permission from rank
                plugin.getRankManager().removePermissionFromRank(rank.getName(), permission);
                
                // Log to audit log
                if (sender instanceof Player) {
                    Player player = (Player) sender;
                    plugin.getAuditManager().logAction(
                        "RANK_PERMISSION_REMOVE",
                        rank.getName(),
                        player.getName(),
                        "Removed permission " + permission + " from rank " + rank.getName(),
                        plugin.getConfigManager().getServerName()
                    );
                } else {
                    plugin.getAuditManager().logAction(
                        "RANK_PERMISSION_REMOVE",
                        rank.getName(),
                        "CONSOLE",
                        "Removed permission " + permission + " from rank " + rank.getName(),
                        plugin.getConfigManager().getServerName()
                    );
                }
                
                // Send success message
                MessageUtils.sendMessage(sender, "admin.rank-permission-remove-success", Map.of(
                    "rank", rank.getName(),
                    "permission", permission
                ));
                
                // Recalculate permissions for all online players with this rank
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    plugin.getServer().getOnlinePlayers().stream()
                        .filter(player -> {
                            try {
                                return plugin.getPermissionManager().hasRank(player, rank.getName());
                            } catch (Exception e) {
                                return false;
                            }
                        })
                        .forEach(player -> plugin.getPermissionManager().calculateAndApplyPermissions(player));
                });
                
            } catch (Exception e) {
                plugin.getLogger().severe("Error removing permission from rank: " + e.getMessage());
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
            // Suggest permissions from the selected rank
            String rankName = args[0];
            Rank rank = plugin.getRankManager().getRank(rankName);
            
            if (rank != null) {
                List<String> permissions = rank.getPermissions().keySet().stream()
                    .collect(Collectors.toList());
                
                String partial = args[1].toLowerCase();
                return permissions.stream()
                    .filter(perm -> perm.toLowerCase().startsWith(partial))
                    .collect(Collectors.toList());
            }
        }
        
        return Collections.emptyList();
    }
} 