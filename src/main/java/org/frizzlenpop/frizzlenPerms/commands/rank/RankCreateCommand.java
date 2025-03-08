package org.frizzlenpop.frizzlenPerms.commands.rank;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.frizzlenpop.frizzlenPerms.FrizzlenPerms;
import org.frizzlenpop.frizzlenPerms.commands.SubCommand;
import org.frizzlenpop.frizzlenPerms.models.Rank;
import org.frizzlenpop.frizzlenPerms.utils.MessageUtils;

import java.util.List;
import java.util.Map;

/**
 * Command to create a new rank.
 */
public class RankCreateCommand implements SubCommand {
    
    private final FrizzlenPerms plugin;
    
    /**
     * Creates a new RankCreateCommand.
     *
     * @param plugin The plugin instance
     */
    public RankCreateCommand(FrizzlenPerms plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public String getName() {
        return "rankcreate";
    }
    
    @Override
    public String getDescription() {
        return "Creates a new rank.";
    }
    
    @Override
    public String getUsage() {
        return "/frizzlenperms rankcreate <name> [displayName] [weight] [color]";
    }
    
    @Override
    public String getPermission() {
        return "frizzlenperms.rank.create";
    }
    
    @Override
    public int getMinArgs() {
        return 1;
    }
    
    @Override
    public List<String> getAliases() {
        return List.of("createrank", "newrank");
    }
    
    @Override
    public boolean execute(CommandSender sender, String[] args) {
        // Get rank name
        String rankName = args[0];
        
        // Check if rank already exists
        if (plugin.getRankManager().getRank(rankName) != null) {
            MessageUtils.sendMessage(sender, "rank.already-exists", Map.of("rank", rankName));
            return true;
        }
        
        // Get optional parameters
        String displayName = rankName;
        int weight = 0;
        String color = "&f";
        
        if (args.length > 1) {
            displayName = args[1];
        }
        
        if (args.length > 2) {
            try {
                weight = Integer.parseInt(args[2]);
            } catch (NumberFormatException e) {
                MessageUtils.sendMessage(sender, "rank.invalid-weight", Map.of("weight", args[2]));
                return false;
            }
        }
        
        if (args.length > 3) {
            color = args[3];
            
            // Validate color
            if (!color.startsWith("&")) {
                color = "&" + color;
            }
            
            // Check if color is valid
            try {
                ChatColor.getByChar(color.charAt(1));
            } catch (Exception e) {
                MessageUtils.sendMessage(sender, "rank.invalid-color", Map.of("color", color));
                return false;
            }
        }
        
        // Create rank
        Rank rank = new Rank(rankName);
        rank.setDisplayName(displayName);
        rank.setWeight(weight);
        rank.setColor(color);
        
        // Save rank
        boolean success = plugin.getRankManager().createRankFromObject(rank);
        
        if (success) {
            MessageUtils.sendMessage(sender, "rank.created", Map.of(
                "rank", rankName,
                "display", displayName,
                "weight", String.valueOf(weight),
                "color", color
            ));
        } else {
            MessageUtils.sendMessage(sender, "rank.create-failed", Map.of("rank", rankName));
        }
        
        return true;
    }
    
    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 3) {
            // Weight suggestions
            return List.of("0", "10", "50", "100");
        } else if (args.length == 4) {
            // Color suggestions
            return List.of("&0", "&1", "&2", "&3", "&4", "&5", "&6", "&7", "&8", "&9", "&a", "&b", "&c", "&d", "&e", "&f");
        }
        
        return List.of();
    }
} 