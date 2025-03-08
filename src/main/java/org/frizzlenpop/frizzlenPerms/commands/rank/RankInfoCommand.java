package org.frizzlenpop.frizzlenPerms.commands.rank;

import org.bukkit.command.CommandSender;
import org.frizzlenpop.frizzlenPerms.FrizzlenPerms;
import org.frizzlenpop.frizzlenPerms.commands.SubCommand;
import org.frizzlenpop.frizzlenPerms.models.Rank;
import org.frizzlenpop.frizzlenPerms.utils.MessageUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Command to show detailed information about a rank.
 */
public class RankInfoCommand implements SubCommand {
    
    private final FrizzlenPerms plugin;
    
    /**
     * Creates a new RankInfoCommand.
     *
     * @param plugin The plugin instance
     */
    public RankInfoCommand(FrizzlenPerms plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public String getName() {
        return "rankinfo";
    }
    
    @Override
    public String getDescription() {
        return "Shows detailed information about a rank.";
    }
    
    @Override
    public String getUsage() {
        return "/frizzlenperms rankinfo <rank>";
    }
    
    @Override
    public String getPermission() {
        return "frizzlenperms.admin.rankinfo";
    }
    
    @Override
    public int getMinArgs() {
        return 1;
    }
    
    @Override
    public List<String> getAliases() {
        return List.of("rinfo", "rinformation");
    }
    
    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (args.length < 1) {
            MessageUtils.sendMessage(sender, "error.missing-arguments", Map.of(
                "usage", getUsage()
            ));
            return false;
        }
        
        String rankName = args[0];
        Rank rank = plugin.getRankManager().getRank(rankName);
        
        if (rank == null) {
            MessageUtils.sendMessage(sender, "error.rank-not-found", Map.of(
                "rank", rankName
            ));
            return false;
        }
        
        // Display rank information
        MessageUtils.sendMessage(sender, "ranks.info-header", Map.of(
            "rank", rank.getDisplayName(),
            "name", rank.getName()
        ));
        
        // Basic info
        MessageUtils.sendMessage(sender, "ranks.info-display-name", Map.of(
            "display_name", rank.getDisplayName()
        ));
        MessageUtils.sendMessage(sender, "ranks.info-prefix", Map.of(
            "prefix", rank.getPrefix() != null ? rank.getPrefix() : "None"
        ));
        MessageUtils.sendMessage(sender, "ranks.info-suffix", Map.of(
            "suffix", rank.getSuffix() != null ? rank.getSuffix() : "None"
        ));
        MessageUtils.sendMessage(sender, "ranks.info-weight", Map.of(
            "weight", String.valueOf(rank.getWeight())
        ));
        MessageUtils.sendMessage(sender, "ranks.info-chat-color", Map.of(
            "chat_color", rank.getColor() != null ? rank.getColor() : "None"
        ));
        MessageUtils.sendMessage(sender, "ranks.info-default", Map.of(
            "default", rank.isDefault() ? "Yes" : "No"
        ));
        MessageUtils.sendMessage(sender, "ranks.info-ladder", Map.of(
            "ladder", rank.getLadder() != null ? rank.getLadder() : "default"
        ));
        
        // Inheritance
        List<String> inheritance = rank.getInheritance();
        if (inheritance.isEmpty()) {
            MessageUtils.sendMessage(sender, "ranks.info-inheritance", Map.of(
                "inheritance", "None"
            ));
        } else {
            MessageUtils.sendMessage(sender, "ranks.info-inheritance", Map.of(
                "inheritance", inheritance.stream()
                    .map(name -> {
                        Rank parent = plugin.getRankManager().getRank(name);
                        return parent != null ? parent.getDisplayName() : name;
                    })
                    .collect(Collectors.joining(", "))
            ));
        }
        
        // Permissions
        Set<String> permissions = rank.getPermissions();
        int permCount = permissions.size();
        
        // Show permissions if requested
        if (args.length > 1 && args[1].equalsIgnoreCase("permissions")) {
            if (permissions.isEmpty()) {
                MessageUtils.sendMessage(sender, "ranks.info-permissions", Map.of(
                    "permissions", "None"
                ));
            } else {
                List<String> sortedPerms = new ArrayList<>(permissions);
                Collections.sort(sortedPerms);
                
                MessageUtils.sendMessage(sender, "ranks.info-permissions", Map.of(
                    "permissions", String.join(", ", sortedPerms)
                ));
            }
        } else {
            // Show how to view permissions
            MessageUtils.sendMessage(sender, "ranks.info-permissions", Map.of(
                "permissions", permCount + " permissions. Use /fp rankinfo " + rank.getName() + " permissions to view them."
            ));
        }
        
        // World permissions
        Map<String, Set<String>> worldPerms = rank.getWorldPermissions();
        if (!worldPerms.isEmpty()) {
            MessageUtils.sendMessage(sender, "ranks.info-world-permissions", Map.of(
                "worlds", worldPerms.entrySet().stream()
                    .map(entry -> entry.getKey() + " (" + entry.getValue().size() + " permissions)")
                    .collect(Collectors.joining(", "))
            ));
            
            // Show world permissions if requested
            if (args.length > 2 && args[1].equalsIgnoreCase("world")) {
                String world = args[2];
                Set<String> worldPermSet = worldPerms.get(world);
                if (worldPermSet != null && !worldPermSet.isEmpty()) {
                    List<String> sortedWorldPerms = new ArrayList<>(worldPermSet);
                    Collections.sort(sortedWorldPerms);
                    
                    MessageUtils.sendMessage(sender, "ranks.info-world-permissions-list", Map.of(
                        "world", world,
                        "permissions", String.join(", ", sortedWorldPerms)
                    ));
                }
            }
        }
        
        return true;
    }
    
    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            // Suggest rank names
            String partial = args[0].toLowerCase();
            return plugin.getRankManager().getAllRankNames().stream()
                .filter(name -> name.toLowerCase().startsWith(partial))
                .collect(Collectors.toList());
        } else if (args.length == 2) {
            // Suggest permission or world view options
            String partial = args[1].toLowerCase();
            return Arrays.asList("permissions", "world").stream()
                .filter(option -> option.startsWith(partial))
                .collect(Collectors.toList());
        } else if (args.length == 3 && args[1].equalsIgnoreCase("world")) {
            // If rank and "world" are specified, suggest world names
            String partial = args[2].toLowerCase();
            String rankName = args[0];
            Rank rank = plugin.getRankManager().getRank(rankName);
            
            if (rank != null) {
                // Suggest worlds from the rank's world permissions
                return rank.getWorldPermissions().keySet().stream()
                    .filter(world -> world.toLowerCase().startsWith(partial))
                    .collect(Collectors.toList());
            }
        }
        
        return Collections.emptyList();
    }
} 