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
 * Command to set a prefix for a rank.
 */
public class RankSetPrefixCommand implements SubCommand {
    
    private final FrizzlenPerms plugin;
    
    /**
     * Creates a new RankSetPrefixCommand.
     *
     * @param plugin The plugin instance
     */
    public RankSetPrefixCommand(FrizzlenPerms plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public String getName() {
        return "ranksetprefix";
    }
    
    @Override
    public String getDescription() {
        return "Sets a prefix for a rank.";
    }
    
    @Override
    public String getUsage() {
        return "/frizzlenperms ranksetprefix <rank> <prefix>";
    }
    
    @Override
    public String getPermission() {
        return "frizzlenperms.admin.ranksetprefix";
    }
    
    @Override
    public int getMinArgs() {
        return 2;
    }
    
    @Override
    public List<String> getAliases() {
        return List.of("setprefix", "rankprefix");
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
        
        // Join remaining arguments for prefix (allows spaces)
        StringBuilder prefixBuilder = new StringBuilder();
        for (int i = 1; i < args.length; i++) {
            prefixBuilder.append(args[i]);
            if (i < args.length - 1) {
                prefixBuilder.append(" ");
            }
        }
        final String prefix = prefixBuilder.toString();
        
        // Check if rank exists
        Rank rank = plugin.getRankManager().getRank(rankName);
        if (rank == null) {
            MessageUtils.sendMessage(sender, "error.rank-not-found", Map.of(
                "rank", rankName
            ));
            return false;
        }
        
        // Set prefix asynchronously
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                String oldPrefix = rank.getPrefix() != null ? rank.getPrefix() : "";
                
                // Set rank prefix
                plugin.getRankManager().setRankPrefix(rank.getName(), prefix);
                
                // Log to audit log
                if (sender instanceof Player) {
                    Player player = (Player) sender;
                    plugin.getAuditManager().logAction(
                        "RANK_SET_PREFIX",
                        rank.getName(),
                        player.getName(),
                        "Set prefix of rank " + rank.getName() + " from '" + oldPrefix + "' to '" + prefix + "'",
                        plugin.getConfigManager().getServerName()
                    );
                } else {
                    plugin.getAuditManager().logAction(
                        "RANK_SET_PREFIX",
                        rank.getName(),
                        "CONSOLE",
                        "Set prefix of rank " + rank.getName() + " from '" + oldPrefix + "' to '" + prefix + "'",
                        plugin.getConfigManager().getServerName()
                    );
                }
                
                // Send success message
                MessageUtils.sendMessage(sender, "admin.rank-set-prefix-success", Map.of(
                    "rank", rank.getName(),
                    "prefix", prefix
                ));
                
                // Update prefix for all online players with this rank
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    plugin.getServer().getOnlinePlayers().stream()
                        .filter(player -> {
                            try {
                                return plugin.getPermissionManager().hasRank(player, rank.getName());
                            } catch (Exception e) {
                                return false;
                            }
                        })
                        .forEach(player -> plugin.getPermissionManager().updatePlayerPrefix(player));
                });
                
            } catch (Exception e) {
                plugin.getLogger().severe("Error setting rank prefix: " + e.getMessage());
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
            // Suggest common prefixes
            List<String> commonPrefixes = Arrays.asList(
                "&4[Admin]", "&c[Mod]", "&e[VIP]", "&a[Member]", 
                "&7[&4Admin&7]", "&7[&cMod&7]", "&7[&eVIP&7]", "&7[&aMember&7]",
                "&8[&4Admin&8]", "&8[&cMod&8]", "&8[&eVIP&8]", "&8[&aMember&8]"
            );
            
            String partial = args[1].toLowerCase();
            return commonPrefixes.stream()
                .filter(prefix -> prefix.toLowerCase().startsWith(partial))
                .collect(Collectors.toList());
        }
        
        return Collections.emptyList();
    }
} 