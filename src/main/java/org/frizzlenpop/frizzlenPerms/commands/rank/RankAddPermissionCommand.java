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
 * Command to add a permission to a rank.
 */
public class RankAddPermissionCommand implements SubCommand {
    
    private final FrizzlenPerms plugin;
    
    /**
     * Creates a new RankAddPermissionCommand.
     *
     * @param plugin The plugin instance
     */
    public RankAddPermissionCommand(FrizzlenPerms plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public String getName() {
        return "rankaddpermission";
    }
    
    @Override
    public String getDescription() {
        return "Adds a permission to a rank.";
    }
    
    @Override
    public String getUsage() {
        return "/frizzlenperms rankaddpermission <rank> <permission> [value]";
    }
    
    @Override
    public String getPermission() {
        return "frizzlenperms.admin.rankaddpermission";
    }
    
    @Override
    public int getMinArgs() {
        return 2;
    }
    
    @Override
    public List<String> getAliases() {
        return List.of("addpermtorole", "addpermissiontorank", "rankpermissionadd");
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
        final boolean value = args.length < 3 || !args[2].equalsIgnoreCase("false");
        
        // Check if rank exists
        Rank rank = plugin.getRankManager().getRank(rankName);
        if (rank == null) {
            MessageUtils.sendMessage(sender, "error.rank-not-found", Map.of(
                "rank", rankName
            ));
            return false;
        }
        
        // Check if permission already exists
        if (rank.hasPermission(permission) && rank.getPermissionValue(permission) == value) {
            MessageUtils.sendMessage(sender, "error.rank-permission-already-exists", Map.of(
                "rank", rank.getName(),
                "permission", permission,
                "value", String.valueOf(value)
            ));
            return false;
        }
        
        // Add permission asynchronously
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                // Add permission to rank
                plugin.getRankManager().addPermissionToRank(rank.getName(), permission, value);
                
                // Log to audit log
                if (sender instanceof Player) {
                    Player player = (Player) sender;
                    plugin.getAuditManager().logAction(
                        "RANK_PERMISSION_ADD",
                        rank.getName(),
                        player.getName(),
                        "Added permission " + permission + " with value " + value + " to rank " + rank.getName(),
                        plugin.getConfigManager().getServerName()
                    );
                } else {
                    plugin.getAuditManager().logAction(
                        "RANK_PERMISSION_ADD",
                        rank.getName(),
                        "CONSOLE",
                        "Added permission " + permission + " with value " + value + " to rank " + rank.getName(),
                        plugin.getConfigManager().getServerName()
                    );
                }
                
                // Send success message
                MessageUtils.sendMessage(sender, "admin.rank-permission-add-success", Map.of(
                    "rank", rank.getName(),
                    "permission", permission,
                    "value", String.valueOf(value)
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
                plugin.getLogger().severe("Error adding permission to rank: " + e.getMessage());
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
            // Suggest common permissions
            List<String> commonPermissions = Arrays.asList(
                "minecraft.command.gamemode",
                "minecraft.command.tp",
                "minecraft.command.teleport",
                "minecraft.command.give",
                "minecraft.command.kick",
                "minecraft.command.ban",
                "bukkit.command.help",
                "bukkit.command.plugins",
                "frizzlenperms.user",
                "frizzlenperms.admin",
                "essentials.help",
                "essentials.home",
                "essentials.tpa",
                "essentials.msg",
                "*"
            );
            
            String partial = args[1].toLowerCase();
            return commonPermissions.stream()
                .filter(perm -> perm.toLowerCase().startsWith(partial))
                .collect(Collectors.toList());
        } else if (args.length == 3) {
            // Suggest true/false
            List<String> values = Arrays.asList("true", "false");
            
            String partial = args[2].toLowerCase();
            return values.stream()
                .filter(value -> value.startsWith(partial))
                .collect(Collectors.toList());
        }
        
        return Collections.emptyList();
    }
} 