package org.frizzlenpop.frizzlenPerms.commands.player;

import org.bukkit.command.CommandSender;
import org.frizzlenpop.frizzlenPerms.FrizzlenPerms;
import org.frizzlenpop.frizzlenPerms.commands.SubCommand;
import org.frizzlenpop.frizzlenPerms.models.Rank;
import org.frizzlenpop.frizzlenPerms.utils.MessageUtils;

import java.util.*;

/**
 * Command to list all available ranks.
 */
public class RanksCommand implements SubCommand {
    
    private final FrizzlenPerms plugin;
    
    /**
     * Creates a new RanksCommand.
     *
     * @param plugin The plugin instance
     */
    public RanksCommand(FrizzlenPerms plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public String getName() {
        return "ranks";
    }
    
    @Override
    public String getDescription() {
        return "Lists all available ranks.";
    }
    
    @Override
    public String getUsage() {
        return "/frizzlenperms ranks";
    }
    
    @Override
    public String getPermission() {
        return "frizzlenperms.player.ranks";
    }
    
    @Override
    public int getMinArgs() {
        return 0;
    }
    
    @Override
    public List<String> getAliases() {
        return List.of("ranklist", "listranks");
    }
    
    @Override
    public boolean execute(CommandSender sender, String[] args) {
        List<Rank> ranks = plugin.getRankManager().getAllRanks();
        
        if (ranks.isEmpty()) {
            MessageUtils.sendMessage(sender, "player.ranks-none");
            return true;
        }
        
        MessageUtils.sendMessage(sender, "player.ranks-header");
        
        // Group ranks by ladder
        Map<String, List<Rank>> ladderRanks = new HashMap<>();
        
        for (Rank rank : ranks) {
            String ladder = rank.getLadder();
            if (ladder == null) {
                ladder = "default";
            }
            
            ladderRanks.computeIfAbsent(ladder, k -> new ArrayList<>()).add(rank);
        }
        
        // Sort ladders alphabetically
        List<String> ladders = new ArrayList<>(ladderRanks.keySet());
        Collections.sort(ladders);
        
        // Display ranks by ladder
        for (String ladder : ladders) {
            List<Rank> ladderRankList = ladderRanks.get(ladder);
            
            // Sort ranks by weight (highest first)
            ladderRankList.sort((r1, r2) -> Integer.compare(r2.getWeight(), r1.getWeight()));
            
            // Display ladder header if not default or multiple ladders
            if (!ladder.equals("default") || ladders.size() > 1) {
                MessageUtils.sendMessage(sender, "player.ranks-ladder-header", Map.of(
                    "ladder", ladder
                ));
            }
            
            // Display ranks
            for (Rank rank : ladderRankList) {
                String defaultMark = rank.isDefault() ? " (Default)" : "";
                
                // Show basic info for regular players
                if (!sender.hasPermission("frizzlenperms.admin.ranks")) {
                    MessageUtils.sendMessage(sender, "player.ranks-entry", Map.of(
                        "rank", rank.getDisplayName(),
                        "prefix", rank.getPrefix(),
                        "default", defaultMark
                    ));
                } 
                // Show detailed info for admins
                else {
                    MessageUtils.sendMessage(sender, "player.ranks-entry-admin", Map.of(
                        "rank", rank.getDisplayName(),
                        "name", rank.getName(),
                        "prefix", rank.getPrefix(),
                        "weight", String.valueOf(rank.getWeight()),
                        "default", defaultMark,
                        "permissions", String.valueOf(rank.getPermissions().size())
                    ));
                }
            }
        }
        
        return true;
    }
    
    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        return Collections.emptyList();
    }
} 