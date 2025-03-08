package org.frizzlenpop.frizzlenPerms.commands.rank;

import org.bukkit.command.CommandSender;
import org.frizzlenpop.frizzlenPerms.FrizzlenPerms;
import org.frizzlenpop.frizzlenPerms.commands.SubCommand;
import org.frizzlenpop.frizzlenPerms.models.Rank;
import org.frizzlenpop.frizzlenPerms.utils.MessageUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Command to list all ranks in the system.
 */
public class RankListCommand implements SubCommand {
    
    private final FrizzlenPerms plugin;
    
    /**
     * Creates a new RankListCommand.
     *
     * @param plugin The plugin instance
     */
    public RankListCommand(FrizzlenPerms plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public String getName() {
        return "ranklist";
    }
    
    @Override
    public String getDescription() {
        return "Lists all ranks in the system.";
    }
    
    @Override
    public String getUsage() {
        return "/frizzlenperms ranklist [page]";
    }
    
    @Override
    public String getPermission() {
        return "frizzlenperms.admin.ranklist";
    }
    
    @Override
    public int getMinArgs() {
        return 0;
    }
    
    @Override
    public List<String> getAliases() {
        return List.of("listranks", "ranks");
    }
    
    @Override
    public boolean execute(CommandSender sender, String[] args) {
        int page = 1;
        if (args.length > 0) {
            try {
                page = Integer.parseInt(args[0]);
                if (page < 1) {
                    page = 1;
                }
            } catch (NumberFormatException e) {
                MessageUtils.sendMessage(sender, "error.invalid-page-number", Map.of(
                    "input", args[0]
                ));
                return false;
            }
        }
        
        // Get all ranks
        List<Rank> ranks = new ArrayList<>(plugin.getRankManager().getRanks());
        
        // Sort ranks by weight (higher weight first)
        ranks.sort(Comparator.comparingInt(Rank::getWeight).reversed());
        
        // Paginate ranks
        int ranksPerPage = 10;
        int totalRanks = ranks.size();
        int maxPage = (int) Math.ceil((double) totalRanks / ranksPerPage);
        
        if (page > maxPage) {
            if (totalRanks == 0) {
                MessageUtils.sendMessage(sender, "admin.rank-list-empty");
                return true;
            } else {
                page = maxPage;
            }
        }
        
        int startIndex = (page - 1) * ranksPerPage;
        int endIndex = Math.min(startIndex + ranksPerPage, totalRanks);
        
        List<Rank> pageRanks = ranks.subList(startIndex, endIndex);
        
        // Send list header
        MessageUtils.sendMessage(sender, "admin.rank-list-header", Map.of(
            "page", String.valueOf(page),
            "max_page", String.valueOf(maxPage),
            "total", String.valueOf(totalRanks)
        ));
        
        // Send rank entries
        for (Rank rank : pageRanks) {
            boolean isDefault = rank.isDefaultRank();
            String prefix = rank.getPrefix() != null ? rank.getPrefix() : "";
            String suffix = rank.getSuffix() != null ? rank.getSuffix() : "";
            
            MessageUtils.sendMessage(sender, "admin.rank-list-entry", Map.of(
                "name", rank.getName(),
                "display_name", rank.getDisplayName(),
                "weight", String.valueOf(rank.getWeight()),
                "default", isDefault ? "Yes" : "No",
                "prefix", prefix,
                "suffix", suffix,
                "color", rank.getColor() != null ? rank.getColor() : ""
            ));
        }
        
        // Send list footer
        if (page < maxPage) {
            MessageUtils.sendMessage(sender, "admin.rank-list-footer", Map.of(
                "command", "/frizzlenperms ranklist " + (page + 1)
            ));
        }
        
        return true;
    }
    
    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            // Suggest page numbers
            List<Rank> ranks = new ArrayList<>(plugin.getRankManager().getRanks());
            int ranksPerPage = 10;
            int maxPage = (int) Math.ceil((double) ranks.size() / ranksPerPage);
            
            List<String> pages = new ArrayList<>();
            for (int i = 1; i <= maxPage; i++) {
                pages.add(String.valueOf(i));
            }
            
            String partial = args[0].toLowerCase();
            return pages.stream()
                .filter(page -> page.startsWith(partial))
                .collect(Collectors.toList());
        }
        
        return Collections.emptyList();
    }
} 