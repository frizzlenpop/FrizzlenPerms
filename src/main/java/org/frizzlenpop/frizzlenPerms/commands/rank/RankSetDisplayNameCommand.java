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
 * Command to set the display name of a rank.
 */
public class RankSetDisplayNameCommand implements SubCommand {
    
    private final FrizzlenPerms plugin;
    
    /**
     * Creates a new RankSetDisplayNameCommand.
     *
     * @param plugin The plugin instance
     */
    public RankSetDisplayNameCommand(FrizzlenPerms plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public String getName() {
        return "ranksetdisplayname";
    }
    
    @Override
    public String getDescription() {
        return "Sets the display name of a rank.";
    }
    
    @Override
    public String getUsage() {
        return "/frizzlenperms ranksetdisplayname <rank> <displayName>";
    }
    
    @Override
    public String getPermission() {
        return "frizzlenperms.admin.ranksetdisplayname";
    }
    
    @Override
    public int getMinArgs() {
        return 2;
    }
    
    @Override
    public List<String> getAliases() {
        return List.of("setdisplayname", "rankdisplayname");
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
        
        // Join remaining arguments for display name (allows spaces)
        StringBuilder displayNameBuilder = new StringBuilder();
        for (int i = 1; i < args.length; i++) {
            displayNameBuilder.append(args[i]);
            if (i < args.length - 1) {
                displayNameBuilder.append(" ");
            }
        }
        final String displayName = displayNameBuilder.toString();
        
        // Check if rank exists
        Rank rank = plugin.getRankManager().getRank(rankName);
        if (rank == null) {
            MessageUtils.sendMessage(sender, "error.rank-not-found", Map.of(
                "rank", rankName
            ));
            return false;
        }
        
        // Check if display name already set to the same value
        if (rank.getDisplayName() != null && rank.getDisplayName().equals(displayName)) {
            MessageUtils.sendMessage(sender, "error.rank-displayname-already-set", Map.of(
                "rank", rank.getName(),
                "displayname", displayName
            ));
            return false;
        }
        
        // Set display name asynchronously
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                String oldDisplayName = rank.getDisplayName() != null ? rank.getDisplayName() : rank.getName();
                
                // Set rank display name
                plugin.getRankManager().setRankDisplayName(rank.getName(), displayName);
                
                // Log to audit log
                if (sender instanceof Player) {
                    Player player = (Player) sender;
                    plugin.getAuditManager().logAction(
                        player.getUniqueId(),
                        player.getName(),
                        org.frizzlenpop.frizzlenPerms.models.AuditLog.ActionType.RANK_MODIFY,
                        rank.getName(),
                        "Set display name of rank " + rank.getName() + " from '" + oldDisplayName + "' to '" + displayName + "'",
                        plugin.getConfigManager().getServerName()
                    );
                } else {
                    plugin.getAuditManager().logAction(
                        null,
                        "Console",
                        org.frizzlenpop.frizzlenPerms.models.AuditLog.ActionType.RANK_MODIFY,
                        rank.getName(),
                        "Set display name of rank " + rank.getName() + " from '" + oldDisplayName + "' to '" + displayName + "'",
                        plugin.getConfigManager().getServerName()
                    );
                }
                
                // Send success message
                MessageUtils.sendMessage(sender, "admin.rank-set-displayname-success", Map.of(
                    "rank", rank.getName(),
                    "displayname", displayName
                ));
                
                // Update display for all online players with this rank
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    plugin.getServer().getOnlinePlayers().stream()
                        .filter(player -> {
                            try {
                                return plugin.getPermissionManager().hasRank(player, rank.getName());
                            } catch (Exception e) {
                                return false;
                            }
                        })
                        .forEach(player -> plugin.getPermissionManager().updateDisplayName(player));
                });
                
            } catch (Exception e) {
                plugin.getLogger().severe("Error setting rank display name: " + e.getMessage());
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
            // Suggest display name based on current rank name
            String rankName = args[0];
            Rank rank = plugin.getRankManager().getRank(rankName);
            
            if (rank != null) {
                String currentName = rank.getName();
                List<String> suggestions = new ArrayList<>();
                
                // Suggest the rank name with color codes
                suggestions.add("&a" + currentName);
                suggestions.add("&b" + currentName);
                suggestions.add("&e" + currentName);
                suggestions.add("&6" + currentName);
                suggestions.add("&c" + currentName);
                suggestions.add("&9" + currentName);
                
                // Suggest capitalized versions
                suggestions.add(currentName.toUpperCase());
                suggestions.add(currentName.substring(0, 1).toUpperCase() + currentName.substring(1));
                
                String partial = args[1];
                return suggestions.stream()
                    .filter(suggestion -> suggestion.toLowerCase().startsWith(partial.toLowerCase()))
                    .collect(Collectors.toList());
            }
        }
        
        return Collections.emptyList();
    }
} 