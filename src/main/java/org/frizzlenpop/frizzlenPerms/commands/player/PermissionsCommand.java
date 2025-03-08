package org.frizzlenpop.frizzlenPerms.commands.player;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.frizzlenpop.frizzlenPerms.FrizzlenPerms;
import org.frizzlenpop.frizzlenPerms.commands.SubCommand;
import org.frizzlenpop.frizzlenPerms.models.PlayerData;
import org.frizzlenpop.frizzlenPerms.models.Rank;
import org.frizzlenpop.frizzlenPerms.utils.MessageUtils;

import java.util.*;

/**
 * Command to list permissions for a player or rank.
 */
public class PermissionsCommand implements SubCommand {
    
    private final FrizzlenPerms plugin;
    
    /**
     * Creates a new PermissionsCommand.
     *
     * @param plugin The plugin instance
     */
    public PermissionsCommand(FrizzlenPerms plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public String getName() {
        return "permissions";
    }
    
    @Override
    public String getDescription() {
        return "Lists permissions for a player or rank.";
    }
    
    @Override
    public String getUsage() {
        return "/frizzlenperms permissions <player|rank> [page]";
    }
    
    @Override
    public String getPermission() {
        return "frizzlenperms.player.permissions";
    }
    
    @Override
    public int getMinArgs() {
        return 1;
    }
    
    @Override
    public List<String> getAliases() {
        return List.of("perms", "perm", "permission");
    }
    
    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (args.length < 1) {
            MessageUtils.sendMessage(sender, "error.missing-arguments", Map.of(
                "usage", getUsage()
            ));
            return false;
        }
        
        String target = args[0];
        int page = 1;
        
        if (args.length > 1) {
            try {
                page = Integer.parseInt(args[1]);
                if (page < 1) {
                    page = 1;
                }
            } catch (NumberFormatException e) {
                MessageUtils.sendMessage(sender, "error.invalid-page");
                return false;
            }
        }
        
        // Check if target is a player
        Player targetPlayer = Bukkit.getPlayer(target);
        if (targetPlayer != null) {
            return showPlayerPermissions(sender, targetPlayer, page);
        }
        
        // Check if target is an offline player
        PlayerData playerData = plugin.getDataManager().getPlayerDataByName(target);
        if (playerData != null) {
            return showOfflinePlayerPermissions(sender, playerData, page);
        }
        
        // Check if target is a rank
        Rank rank = plugin.getRankManager().getRank(target);
        if (rank != null) {
            return showRankPermissions(sender, rank, page);
        }
        
        // Target not found
        MessageUtils.sendMessage(sender, "error.target-not-found", Map.of(
            "target", target
        ));
        return false;
    }
    
    /**
     * Shows permissions for an online player.
     *
     * @param sender The command sender
     * @param player The target player
     * @param page The page number
     * @return Whether the operation was successful
     */
    private boolean showPlayerPermissions(CommandSender sender, Player player, int page) {
        // Check permission if checking another player
        if (sender instanceof Player && !sender.getName().equals(player.getName()) && 
            !sender.hasPermission("frizzlenperms.admin.permissions")) {
            MessageUtils.sendMessage(sender, "error.no-permission-other");
            return false;
        }
        
        PlayerData playerData = plugin.getDataManager().getPlayerData(player.getUniqueId());
        if (playerData == null) {
            MessageUtils.sendMessage(sender, "error.player-not-found", Map.of(
                "player", player.getName()
            ));
            return false;
        }
        
        // Get all permissions
        Set<String> permissions = new HashSet<>(playerData.getPermissions());
        
        // Add rank permissions
        String primaryRank = playerData.getPrimaryRank();
        if (primaryRank != null) {
            Rank rank = plugin.getRankManager().getRank(primaryRank);
            if (rank != null) {
                permissions.addAll(rank.getPermissions());
            }
        }
        
        for (String rankName : playerData.getSecondaryRanks()) {
            Rank rank = plugin.getRankManager().getRank(rankName);
            if (rank != null) {
                permissions.addAll(rank.getPermissions());
            }
        }
        
        // Display permissions
        return displayPermissions(sender, player.getName(), permissions, page);
    }
    
    /**
     * Shows permissions for an offline player.
     *
     * @param sender The command sender
     * @param playerData The target player data
     * @param page The page number
     * @return Whether the operation was successful
     */
    private boolean showOfflinePlayerPermissions(CommandSender sender, PlayerData playerData, int page) {
        // Check permission if checking another player
        if (sender instanceof Player && !sender.getName().equals(playerData.getPlayerName()) && 
            !sender.hasPermission("frizzlenperms.admin.permissions")) {
            MessageUtils.sendMessage(sender, "error.no-permission-other");
            return false;
        }
        
        // Get all permissions
        Set<String> permissions = new HashSet<>(playerData.getPermissions());
        
        // Add rank permissions
        String primaryRank = playerData.getPrimaryRank();
        if (primaryRank != null) {
            Rank rank = plugin.getRankManager().getRank(primaryRank);
            if (rank != null) {
                permissions.addAll(rank.getPermissions());
            }
        }
        
        for (String rankName : playerData.getSecondaryRanks()) {
            Rank rank = plugin.getRankManager().getRank(rankName);
            if (rank != null) {
                permissions.addAll(rank.getPermissions());
            }
        }
        
        // Display permissions
        return displayPermissions(sender, playerData.getPlayerName(), permissions, page);
    }
    
    /**
     * Shows permissions for a rank.
     *
     * @param sender The command sender
     * @param rank The target rank
     * @param page The page number
     * @return Whether the operation was successful
     */
    private boolean showRankPermissions(CommandSender sender, Rank rank, int page) {
        // Get all permissions
        Set<String> permissions = new HashSet<>(rank.getPermissions());
        
        // Add inherited permissions
        for (String inheritedRankName : rank.getInheritance()) {
            Rank inheritedRank = plugin.getRankManager().getRank(inheritedRankName);
            if (inheritedRank != null) {
                permissions.addAll(inheritedRank.getPermissions());
            }
        }
        
        // Display permissions
        return displayPermissions(sender, rank.getDisplayName() + " (Rank)", permissions, page);
    }
    
    /**
     * Displays a paginated list of permissions.
     *
     * @param sender The command sender
     * @param targetName The name of the target
     * @param permissions The permissions to display
     * @param page The page number
     * @return Whether the operation was successful
     */
    private boolean displayPermissions(CommandSender sender, String targetName, Set<String> permissions, int page) {
        List<String> permList = new ArrayList<>(permissions);
        Collections.sort(permList);
        
        int perPage = 10;
        int totalPages = (int) Math.ceil((double) permList.size() / perPage);
        
        if (totalPages == 0) {
            totalPages = 1;
        }
        
        if (page > totalPages) {
            page = totalPages;
        }
        
        int start = (page - 1) * perPage;
        int end = Math.min(start + perPage, permList.size());
        
        MessageUtils.sendMessage(sender, "player.permissions-header", Map.of(
            "target", targetName,
            "page", String.valueOf(page),
            "total", String.valueOf(totalPages),
            "count", String.valueOf(permissions.size())
        ));
        
        if (permissions.isEmpty()) {
            MessageUtils.sendMessage(sender, "player.permissions-none");
            return true;
        }
        
        for (int i = start; i < end; i++) {
            String permission = permList.get(i);
            MessageUtils.sendMessage(sender, "player.permission-entry", Map.of(
                "permission", permission
            ));
        }
        
        if (page < totalPages) {
            MessageUtils.sendMessage(sender, "player.permissions-next-page", Map.of(
                "command", "/frizzlenperms permissions " + targetName + " " + (page + 1)
            ));
        }
        
        return true;
    }
    
    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            String partial = args[0].toLowerCase();
            List<String> suggestions = new ArrayList<>();
            
            // Add online players
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (player.getName().toLowerCase().startsWith(partial)) {
                    suggestions.add(player.getName());
                }
            }
            
            // Add ranks
            for (String rankName : plugin.getRankManager().getAllRankNames()) {
                if (rankName.toLowerCase().startsWith(partial)) {
                    suggestions.add(rankName);
                }
            }
            
            return suggestions;
        }
        
        return Collections.emptyList();
    }
} 