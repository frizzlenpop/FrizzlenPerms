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
 * Command to add inheritance to a rank.
 */
public class RankAddInheritanceCommand implements SubCommand {
    
    private final FrizzlenPerms plugin;
    
    /**
     * Creates a new RankAddInheritanceCommand.
     *
     * @param plugin The plugin instance
     */
    public RankAddInheritanceCommand(FrizzlenPerms plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public String getName() {
        return "rankaddinheritance";
    }
    
    @Override
    public String getDescription() {
        return "Adds inheritance to a rank.";
    }
    
    @Override
    public String getUsage() {
        return "/frizzlenperms rankaddinheritance <rank> <parent>";
    }
    
    @Override
    public String getPermission() {
        return "frizzlenperms.admin.rankaddinheritance";
    }
    
    @Override
    public int getMinArgs() {
        return 2;
    }
    
    @Override
    public List<String> getAliases() {
        return List.of("addinheritance", "rankinherit");
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
        
        // Check if rank is trying to inherit itself
        if (rankName.equals(parentName)) {
            MessageUtils.sendMessage(sender, "error.rank-inherit-self", Map.of(
                "rank", rankName
            ));
            return false;
        }
        
        // Check if inheritance already exists
        if (rank.getInheritance().contains(parentName)) {
            MessageUtils.sendMessage(sender, "error.rank-already-inherits", Map.of(
                "rank", rankName,
                "parent", parentName
            ));
            return false;
        }
        
        // Check for circular inheritance
        if (hasCircularInheritance(rankName, parentName)) {
            MessageUtils.sendMessage(sender, "error.circular-inheritance", Map.of(
                "rank", rankName,
                "parent", parentName
            ));
            return false;
        }
        
        // Add inheritance asynchronously
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                // Add inheritance
                rank.addInheritance(parentName);
                plugin.getRankManager().addRankPermission(rankName, "inherit." + parentName, sender instanceof Player ? (Player) sender : null);
                
                // Log to audit log
                if (sender instanceof Player) {
                    Player player = (Player) sender;
                    plugin.getAuditManager().logAction(
                        player.getUniqueId(),
                        player.getName(),
                        AuditLog.ActionType.RANK_MODIFY,
                        rankName,
                        "Added inheritance from " + parentName,
                        plugin.getConfigManager().getServerName()
                    );
                } else {
                    plugin.getAuditManager().logAction(
                        null,
                        "Console",
                        AuditLog.ActionType.RANK_MODIFY,
                        rankName,
                        "Added inheritance from " + parentName,
                        plugin.getConfigManager().getServerName()
                    );
                }
                
                // Send success message
                MessageUtils.sendMessage(sender, "admin.rank-add-inheritance-success", Map.of(
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
                plugin.getLogger().severe("Error adding rank inheritance: " + e.getMessage());
                e.printStackTrace();
                MessageUtils.sendMessage(sender, "error.database-error");
            }
        });
        
        return true;
    }
    
    /**
     * Checks if adding an inheritance would create a circular inheritance.
     *
     * @param rankName The rank name
     * @param parentName The parent rank name
     * @return True if it would create a circular inheritance
     */
    private boolean hasCircularInheritance(String rankName, String parentName) {
        Set<String> visited = new HashSet<>();
        Queue<String> queue = new LinkedList<>();
        
        // Start with the parent rank
        queue.add(parentName);
        
        while (!queue.isEmpty()) {
            String current = queue.poll();
            
            if (visited.contains(current)) {
                continue;
            }
            
            visited.add(current);
            
            // If we find the original rank in the inheritance chain, it's circular
            if (current.equals(rankName)) {
                return true;
            }
            
            // Add all inheritances of the current rank to the queue
            Rank currentRank = plugin.getRankManager().getRank(current);
            if (currentRank != null) {
                for (String inheritance : currentRank.getInheritance()) {
                    queue.add(inheritance);
                }
            }
        }
        
        return false;
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
            // Suggest parent rank names (excluding the selected rank and ranks that already inherit from it)
            String rankName = args[0];
            Rank rank = plugin.getRankManager().getRank(rankName);
            
            if (rank != null) {
                List<String> possibleParents = plugin.getRankManager().getAllRanks().stream()
                    .filter(r -> !r.getName().equals(rankName) && 
                                !rank.getInheritance().contains(r.getName()))
                    .map(Rank::getName)
                    .collect(Collectors.toList());
                
                String partial = args[1].toLowerCase();
                return possibleParents.stream()
                    .filter(name -> name.toLowerCase().startsWith(partial))
                    .collect(Collectors.toList());
            }
        }
        
        return Collections.emptyList();
    }
} 