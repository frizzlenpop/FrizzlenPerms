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
 * Command to set a suffix for a rank.
 */
public class RankSetSuffixCommand implements SubCommand {
    
    private final FrizzlenPerms plugin;
    
    /**
     * Creates a new RankSetSuffixCommand.
     *
     * @param plugin The plugin instance
     */
    public RankSetSuffixCommand(FrizzlenPerms plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public String getName() {
        return "ranksetsuffix";
    }
    
    @Override
    public String getDescription() {
        return "Sets a suffix for a rank.";
    }
    
    @Override
    public String getUsage() {
        return "/frizzlenperms ranksetsuffix <rank> <suffix>";
    }
    
    @Override
    public String getPermission() {
        return "frizzlenperms.admin.ranksetsuffix";
    }
    
    @Override
    public int getMinArgs() {
        return 2;
    }
    
    @Override
    public List<String> getAliases() {
        return List.of("setsuffix", "ranksuffix");
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
        
        // Join remaining arguments for suffix (allows spaces)
        StringBuilder suffixBuilder = new StringBuilder();
        for (int i = 1; i < args.length; i++) {
            suffixBuilder.append(args[i]);
            if (i < args.length - 1) {
                suffixBuilder.append(" ");
            }
        }
        final String suffix = suffixBuilder.toString();
        
        // Check if rank exists
        Rank rank = plugin.getRankManager().getRank(rankName);
        if (rank == null) {
            MessageUtils.sendMessage(sender, "error.rank-not-found", Map.of(
                "rank", rankName
            ));
            return false;
        }
        
        // Set suffix asynchronously
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                String oldSuffix = rank.getSuffix() != null ? rank.getSuffix() : "";
                
                // Set rank suffix
                plugin.getRankManager().setRankSuffix(rank.getName(), suffix);
                
                // Log to audit log
                if (sender instanceof Player) {
                    Player player = (Player) sender;
                    plugin.getAuditManager().logAction(
                        player.getUniqueId(),
                        player.getName(),
                        org.frizzlenpop.frizzlenPerms.models.AuditLog.ActionType.RANK_MODIFY,
                        rank.getName(),
                        "Set suffix of rank " + rank.getName() + " from '" + oldSuffix + "' to '" + suffix + "'",
                        plugin.getConfigManager().getServerName()
                    );
                } else {
                    plugin.getAuditManager().logAction(
                        null,
                        "Console",
                        org.frizzlenpop.frizzlenPerms.models.AuditLog.ActionType.RANK_MODIFY,
                        rank.getName(),
                        "Set suffix of rank " + rank.getName() + " from '" + oldSuffix + "' to '" + suffix + "'",
                        plugin.getConfigManager().getServerName()
                    );
                }
                
                // Send success message
                MessageUtils.sendMessage(sender, "admin.rank-set-suffix-success", Map.of(
                    "rank", rank.getName(),
                    "suffix", suffix
                ));
                
                // Update suffix for all online players with this rank
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    plugin.getServer().getOnlinePlayers().stream()
                        .filter(player -> {
                            try {
                                return plugin.getPermissionManager().hasRank(player, rank.getName());
                            } catch (Exception e) {
                                return false;
                            }
                        })
                        .forEach(player -> plugin.getPermissionManager().updatePlayerSuffix(player));
                });
                
            } catch (Exception e) {
                plugin.getLogger().severe("Error setting rank suffix: " + e.getMessage());
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
            // Suggest common suffixes
            List<String> commonSuffixes = Arrays.asList(
                " &8✶", " &e✶", " &6✦", " &b✧", " &a♦",
                " &7(Staff)", " &7(Mod)", " &7(VIP)", " &7(Premium)",
                " &8#1", " &e#1", " &6#1", " &b#1", " &a#1"
            );
            
            String partial = args[1].toLowerCase();
            return commonSuffixes.stream()
                .filter(suffix -> suffix.toLowerCase().startsWith(partial))
                .collect(Collectors.toList());
        }
        
        return Collections.emptyList();
    }
} 