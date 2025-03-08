package org.frizzlenpop.frizzlenPerms.commands.rank;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.frizzlenpop.frizzlenPerms.FrizzlenPerms;
import org.frizzlenpop.frizzlenPerms.commands.SubCommand;
import org.frizzlenpop.frizzlenPerms.models.AuditLog;
import org.frizzlenpop.frizzlenPerms.models.Rank;
import org.frizzlenpop.frizzlenPerms.utils.MessageUtils;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Command to set a color for a rank.
 */
public class RankSetColorCommand implements SubCommand {
    
    private final FrizzlenPerms plugin;
    private final Pattern colorPattern = Pattern.compile("^&[0-9a-fA-F]$");
    
    /**
     * Creates a new RankSetColorCommand.
     *
     * @param plugin The plugin instance
     */
    public RankSetColorCommand(FrizzlenPerms plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public String getName() {
        return "ranksetcolor";
    }
    
    @Override
    public String getDescription() {
        return "Sets a color for a rank.";
    }
    
    @Override
    public String getUsage() {
        return "/frizzlenperms ranksetcolor <rank> <color>";
    }
    
    @Override
    public String getPermission() {
        return "frizzlenperms.admin.ranksetcolor";
    }
    
    @Override
    public int getMinArgs() {
        return 2;
    }
    
    @Override
    public List<String> getAliases() {
        return List.of("setcolor", "rankcolor");
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
        final String color = args[1];
        
        // Check if color code is valid
        if (!colorPattern.matcher(color).matches()) {
            MessageUtils.sendMessage(sender, "error.invalid-color-code", Map.of(
                "color", color
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
        
        // Check if color is already set to the same value
        if (color.equals(rank.getColor())) {
            MessageUtils.sendMessage(sender, "error.rank-color-already-set", Map.of(
                "rank", rankName,
                "color", color
            ));
            return false;
        }
        
        // Set color asynchronously
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                String oldColor = rank.getColor() != null ? rank.getColor() : "";
                
                // Set rank color
                plugin.getRankManager().setRankColor(rank.getName(), color);
                
                // Log to audit log
                if (sender instanceof Player) {
                    Player player = (Player) sender;
                    plugin.getAuditManager().logAction(
                        player.getUniqueId(),
                        player.getName(),
                        AuditLog.ActionType.RANK_MODIFY,
                        rankName,
                        "Set color of rank " + rankName + " from '" + oldColor + "' to '" + color + "'",
                        plugin.getConfigManager().getServerName()
                    );
                } else {
                    plugin.getAuditManager().logAction(
                        null,
                        "Console",
                        AuditLog.ActionType.RANK_MODIFY,
                        rankName,
                        "Set color of rank " + rankName + " from '" + oldColor + "' to '" + color + "'",
                        plugin.getConfigManager().getServerName()
                    );
                }
                
                // Send success message
                MessageUtils.sendMessage(sender, "admin.rank-set-color-success", Map.of(
                    "rank", rankName,
                    "color", color
                ));
                
                // Update display for all online players with this rank
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    plugin.getServer().getOnlinePlayers().stream()
                        .filter(player -> plugin.getPermissionManager().hasRank(player, rank.getName()))
                        .forEach(player -> plugin.getPermissionManager().updatePlayerPrefix(player));
                });
                
            } catch (Exception e) {
                plugin.getLogger().severe("Error setting rank color: " + e.getMessage());
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
            // Suggest common color codes
            List<String> colorCodes = Arrays.asList(
                "&0", "&1", "&2", "&3", "&4", "&5", "&6", "&7", 
                "&8", "&9", "&a", "&b", "&c", "&d", "&e", "&f"
            );
            
            String partial = args[1].toLowerCase();
            return colorCodes.stream()
                .filter(code -> code.toLowerCase().startsWith(partial))
                .collect(Collectors.toList());
        }
        
        return Collections.emptyList();
    }
} 