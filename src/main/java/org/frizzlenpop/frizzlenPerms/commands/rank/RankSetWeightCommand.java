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
 * Command to set the weight of a rank.
 */
public class RankSetWeightCommand implements SubCommand {
    
    private final FrizzlenPerms plugin;
    
    /**
     * Creates a new RankSetWeightCommand.
     *
     * @param plugin The plugin instance
     */
    public RankSetWeightCommand(FrizzlenPerms plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public String getName() {
        return "ranksetweight";
    }
    
    @Override
    public String getDescription() {
        return "Sets the weight of a rank (higher weight = higher priority).";
    }
    
    @Override
    public String getUsage() {
        return "/frizzlenperms ranksetweight <rank> <weight>";
    }
    
    @Override
    public String getPermission() {
        return "frizzlenperms.admin.ranksetweight";
    }
    
    @Override
    public int getMinArgs() {
        return 2;
    }
    
    @Override
    public List<String> getAliases() {
        return List.of("setweight", "rankweight");
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
        
        // Parse weight
        int weight;
        try {
            weight = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            MessageUtils.sendMessage(sender, "error.invalid-number", Map.of(
                "input", args[1]
            ));
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
        
        // Check if weight already set to the same value
        if (rank.getWeight() == weight) {
            MessageUtils.sendMessage(sender, "error.rank-weight-already-set", Map.of(
                "rank", rank.getName(),
                "weight", String.valueOf(weight)
            ));
            return false;
        }
        
        final int oldWeight = rank.getWeight();
        
        // Set weight asynchronously
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                // Set rank weight
                plugin.getRankManager().setRankWeight(rank.getName(), weight);
                
                // Log to audit log
                if (sender instanceof Player) {
                    Player player = (Player) sender;
                    plugin.getAuditManager().logAction(
                        "RANK_SET_WEIGHT",
                        rank.getName(),
                        player.getName(),
                        "Set weight of rank " + rank.getName() + " from " + oldWeight + " to " + weight,
                        plugin.getConfigManager().getServerName()
                    );
                } else {
                    plugin.getAuditManager().logAction(
                        "RANK_SET_WEIGHT",
                        rank.getName(),
                        "CONSOLE",
                        "Set weight of rank " + rank.getName() + " from " + oldWeight + " to " + weight,
                        plugin.getConfigManager().getServerName()
                    );
                }
                
                // Send success message
                MessageUtils.sendMessage(sender, "admin.rank-set-weight-success", Map.of(
                    "rank", rank.getName(),
                    "weight", String.valueOf(weight)
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
                plugin.getLogger().severe("Error setting rank weight: " + e.getMessage());
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
            // Suggest common weights
            Rank rank = plugin.getRankManager().getRank(args[0]);
            if (rank != null) {
                int currentWeight = rank.getWeight();
                List<String> suggestions = new ArrayList<>();
                
                // Suggest weight increments
                suggestions.add(String.valueOf(currentWeight + 1));
                suggestions.add(String.valueOf(currentWeight + 5));
                suggestions.add(String.valueOf(currentWeight + 10));
                
                // Suggest weight decrements
                suggestions.add(String.valueOf(Math.max(0, currentWeight - 1)));
                suggestions.add(String.valueOf(Math.max(0, currentWeight - 5)));
                suggestions.add(String.valueOf(Math.max(0, currentWeight - 10)));
                
                // Suggest common preset weights
                suggestions.add("0");  // Lowest
                suggestions.add("10"); // Low
                suggestions.add("50"); // Medium
                suggestions.add("100"); // High
                suggestions.add("1000"); // Highest
                
                String partial = args[1];
                return suggestions.stream()
                    .filter(w -> w.startsWith(partial))
                    .collect(Collectors.toList());
            }
        }
        
        return Collections.emptyList();
    }
} 