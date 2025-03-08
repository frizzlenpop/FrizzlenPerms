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
 * Command to set a rank as the default rank for new players.
 */
public class RankSetDefaultCommand implements SubCommand {
    
    private final FrizzlenPerms plugin;
    
    /**
     * Creates a new RankSetDefaultCommand.
     *
     * @param plugin The plugin instance
     */
    public RankSetDefaultCommand(FrizzlenPerms plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public String getName() {
        return "ranksetdefault";
    }
    
    @Override
    public String getDescription() {
        return "Sets a rank as the default rank for new players.";
    }
    
    @Override
    public String getUsage() {
        return "/frizzlenperms ranksetdefault <rank>";
    }
    
    @Override
    public String getPermission() {
        return "frizzlenperms.admin.ranksetdefault";
    }
    
    @Override
    public int getMinArgs() {
        return 1;
    }
    
    @Override
    public List<String> getAliases() {
        return List.of("setdefaultrank", "defaultrank");
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
        
        // Check if already default
        if (rank.isDefault()) {
            MessageUtils.sendMessage(sender, "error.rank-already-default", Map.of(
                "rank", rank.getName()
            ));
            return false;
        }
        
        // Set as default asynchronously
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                String oldDefaultName = "";
                // Find current default rank (if any)
                for (Rank r : plugin.getRankManager().getAllRanks()) {
                    if (r.isDefault()) {
                        oldDefaultName = r.getName();
                        break;
                    }
                }
                
                // Set as default
                plugin.getRankManager().setDefaultRank(rank.getName(), sender instanceof Player ? (Player) sender : null);
                
                // Log to audit log
                if (sender instanceof Player) {
                    Player player = (Player) sender;
                    plugin.getAuditManager().logAction(
                        player.getUniqueId(),
                        player.getName(),
                        org.frizzlenpop.frizzlenPerms.models.AuditLog.ActionType.RANK_MODIFY,
                        rank.getName(),
                        "Set rank " + rank.getName() + " as default, replacing " + 
                            (oldDefaultName.isEmpty() ? "none" : oldDefaultName),
                        plugin.getConfigManager().getServerName()
                    );
                } else {
                    plugin.getAuditManager().logAction(
                        null,
                        "Console",
                        org.frizzlenpop.frizzlenPerms.models.AuditLog.ActionType.RANK_MODIFY,
                        rank.getName(),
                        "Set rank " + rank.getName() + " as default, replacing " +
                            (oldDefaultName.isEmpty() ? "none" : oldDefaultName),
                        plugin.getConfigManager().getServerName()
                    );
                }
                
                // Send success message
                MessageUtils.sendMessage(sender, "admin.rank-set-default-success", Map.of(
                    "rank", rank.getName()
                ));
                
            } catch (Exception e) {
                plugin.getLogger().severe("Error setting default rank: " + e.getMessage());
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
                .filter(rank -> !rank.isDefault()) // Filter out ranks that are already default
                .map(Rank::getName)
                .collect(Collectors.toList());
            
            String partial = args[0].toLowerCase();
            return rankNames.stream()
                .filter(name -> name.toLowerCase().startsWith(partial))
                .collect(Collectors.toList());
        }
        
        return Collections.emptyList();
    }
} 