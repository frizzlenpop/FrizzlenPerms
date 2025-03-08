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
        if (args.length < 1) {
            MessageUtils.sendMessage(sender, "error.missing-arguments", Map.of(
                "usage", getUsage()
            ));
            return false;
        }

        // Get rank name and validate format
        final String rankName = args[0];
        if (!rankName.matches("^[a-zA-Z0-9_-]{1,32}$")) {
            MessageUtils.sendMessage(sender, "error.invalid-rank-name", Map.of(
                "rank", rankName,
                "format", "letters, numbers, underscores, and hyphens (max 32 characters)"
            ));
            return false;
        }
        
        // Check if rank already exists
        if (plugin.getRankManager().getRank(rankName) != null) {
            MessageUtils.sendMessage(sender, "ranks.rank-exists", Map.of("rank", rankName));
            return true;
        }
        
        // Get optional parameters
        String displayName = rankName;
        int weight = 0;
        String color = "&f";
        
        if (args.length > 1) {
            displayName = args[1];
            if (displayName.length() > 48) {
                MessageUtils.sendMessage(sender, "error.display-name-too-long", Map.of(
                    "max", "48"
                ));
                return false;
            }
        }
        
        if (args.length > 2) {
            try {
                weight = Integer.parseInt(args[2]);
                if (weight < -999 || weight > 999) {
                    MessageUtils.sendMessage(sender, "error.weight-out-of-range", Map.of(
                        "min", "-999",
                        "max", "999"
                    ));
                    return false;
                }
            } catch (NumberFormatException e) {
                MessageUtils.sendMessage(sender, "error.invalid-weight", Map.of("weight", args[2]));
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
            if (color.length() != 2 || !isValidColorCode(color.charAt(1))) {
                MessageUtils.sendMessage(sender, "error.invalid-color", Map.of("color", color));
                return false;
            }
        }
        
        // Create rank with final variables for async use
        final String finalDisplayName = displayName;
        final int finalWeight = weight;
        final String finalColor = color;
        
        // Create rank
        Rank rank = new Rank(rankName);
        rank.setDisplayName(finalDisplayName);
        rank.setWeight(finalWeight);
        rank.setColor(finalColor);
        
        // Save rank asynchronously
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                boolean success = plugin.getRankManager().createRankFromObject(rank);
                
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    if (success) {
                        MessageUtils.sendMessage(sender, "ranks.create-success", Map.of(
                            "rank", rankName,
                            "display", finalDisplayName,
                            "weight", String.valueOf(finalWeight),
                            "color", finalColor
                        ));
                    } else {
                        MessageUtils.sendMessage(sender, "ranks.create-failed", Map.of("rank", rankName));
                    }
                });
            } catch (Exception e) {
                plugin.getLogger().severe("Error creating rank: " + e.getMessage());
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    MessageUtils.sendMessage(sender, "error.internal-error");
                });
            }
        });
        
        return true;
    }
    
    /**
     * Checks if a character is a valid color code.
     *
     * @param c The character to check
     * @return Whether the character is a valid color code
     */
    private boolean isValidColorCode(char c) {
        return "0123456789AaBbCcDdEeFfKkLlMmNnOoRr".indexOf(c) > -1;
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