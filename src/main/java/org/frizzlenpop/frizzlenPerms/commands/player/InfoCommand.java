package org.frizzlenpop.frizzlenPerms.commands.player;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.frizzlenpop.frizzlenPerms.FrizzlenPerms;
import org.frizzlenpop.frizzlenPerms.commands.SubCommand;
import org.frizzlenpop.frizzlenPerms.data.DataManager;
import org.frizzlenpop.frizzlenPerms.permissions.PermissionManager;
import org.frizzlenpop.frizzlenPerms.ranks.RankManager;
import org.frizzlenpop.frizzlenPerms.models.PlayerData;
import org.frizzlenpop.frizzlenPerms.models.Rank;
import org.frizzlenpop.frizzlenPerms.utils.MessageUtils;
import org.frizzlenpop.frizzlenPerms.utils.TimeUtils;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Command to view player permission information.
 */
public class InfoCommand implements SubCommand {
    
    private final FrizzlenPerms plugin;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    
    /**
     * Creates a new InfoCommand.
     *
     * @param plugin The plugin instance
     */
    public InfoCommand(FrizzlenPerms plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public String getName() {
        return "info";
    }
    
    @Override
    public String getDescription() {
        return "Shows permission information for a player.";
    }
    
    @Override
    public String getUsage() {
        return "/frizzlenperms info [player]";
    }
    
    @Override
    public String getPermission() {
        return "frizzlenperms.player.info";
    }
    
    @Override
    public int getMinArgs() {
        return 0;
    }
    
    @Override
    public List<String> getAliases() {
        return List.of("i", "player", "check", "who");
    }
    
    @Override
    public boolean execute(CommandSender sender, String[] args) {
        String playerName;
        
        // Determine the target player
        if (args.length == 0) {
            // No args, check if sender is a player
            if (!(sender instanceof Player)) {
                MessageUtils.sendMessage(sender, "error.specify-player");
                return false;
            }
            playerName = sender.getName();
        } else {
            // Target another player
            playerName = args[0];
            
            // Check permission if checking another player
            if (!sender.getName().equalsIgnoreCase(playerName) && 
                !sender.hasPermission("frizzlenperms.admin.info")) {
                MessageUtils.sendMessage(sender, "error.no-permission-other");
                return false;
            }
        }
        
        // Get player data
        PlayerData playerData = plugin.getDataManager().getPlayerDataByName(playerName);
        if (playerData == null) {
            MessageUtils.sendMessage(sender, "error.player-not-found", Map.of("player", playerName));
            return true;
        }
        
        // Send player info
        MessageUtils.sendMessage(sender, "player.info-header", Map.of("player", playerData.getPlayerName()));
        
        // Primary rank
        Rank primaryRank = null;
        if (playerData.getPrimaryRank() != null) {
            primaryRank = plugin.getRankManager().getRank(playerData.getPrimaryRank());
        }
        
        MessageUtils.sendMessage(sender, "player.info-primary-rank", Map.of(
            "rank", primaryRank != null ? primaryRank.getDisplayName() : "None"
        ));
        
        // Secondary ranks
        List<String> secondaryRanks = playerData.getSecondaryRanks();
        if (secondaryRanks.isEmpty()) {
            MessageUtils.sendMessage(sender, "player.info-secondary-ranks-none");
        } else {
            List<String> rankNames = new ArrayList<>();
            for (String rankName : secondaryRanks) {
                Rank rank = plugin.getRankManager().getRank(rankName);
                if (rank != null) {
                    rankNames.add(rank.getDisplayName());
                }
            }
            
            MessageUtils.sendMessage(sender, "player.info-secondary-ranks", Map.of(
                "ranks", String.join(", ", rankNames)
            ));
        }
        
        // Show permissions if admin
        if (sender.hasPermission("frizzlenperms.admin.info")) {
            // Direct permissions
            Set<String> permissions = playerData.getPermissions();
            if (permissions.isEmpty()) {
                MessageUtils.sendMessage(sender, "player.info-permissions-none");
            } else {
                MessageUtils.sendMessage(sender, "player.info-permissions-header");
                List<String> sortedPerms = new ArrayList<>(permissions);
                Collections.sort(sortedPerms);
                for (String permission : sortedPerms) {
                    MessageUtils.sendMessage(sender, "player.info-permission-entry", Map.of(
                        "permission", permission
                    ));
                }
            }
            
            // Temporary ranks
            Map<String, Long> tempRanks = playerData.getTemporaryRanks();
            if (!tempRanks.isEmpty()) {
                MessageUtils.sendMessage(sender, "player.info-temp-ranks-header");
                for (Map.Entry<String, Long> entry : tempRanks.entrySet()) {
                    Rank rank = plugin.getRankManager().getRank(entry.getKey());
                    String rankName = rank != null ? rank.getDisplayName() : entry.getKey();
                    long expiry = entry.getValue();
                    long remaining = expiry - System.currentTimeMillis();
                    
                    if (remaining > 0) {
                        String timeStr = formatTimeRemaining(remaining);
                        MessageUtils.sendMessage(sender, "player.info-temp-rank-entry", Map.of(
                            "rank", rankName,
                            "time", timeStr
                        ));
                    }
                }
            }
            
            // Temporary permissions
            Map<String, Long> tempPerms = playerData.getTemporaryPermissions();
            if (!tempPerms.isEmpty()) {
                MessageUtils.sendMessage(sender, "player.info-temp-perms-header");
                for (Map.Entry<String, Long> entry : tempPerms.entrySet()) {
                    String permission = entry.getKey();
                    long expiry = entry.getValue();
                    long remaining = expiry - System.currentTimeMillis();
                    
                    if (remaining > 0) {
                        String timeStr = formatTimeRemaining(remaining);
                        MessageUtils.sendMessage(sender, "player.info-temp-perm-entry", Map.of(
                            "permission", permission,
                            "time", timeStr
                        ));
                    }
                }
            }
        }
        
        // Show Discord link status if enabled
        if (plugin.getConfigManager().isDiscordEnabled()) {
            String discordId = playerData.getDiscordId();
            boolean linked = discordId != null && !discordId.isEmpty();
            
            MessageUtils.sendMessage(sender, linked ? 
                "player.info-discord-linked" : "player.info-discord-not-linked");
        }
        
        return true;
    }
    
    /**
     * Formats time remaining in a human-readable format.
     *
     * @param millis Time in milliseconds
     * @return Formatted time string
     */
    private String formatTimeRemaining(long millis) {
        long seconds = millis / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;
        
        if (days > 0) {
            return days + " day" + (days > 1 ? "s" : "");
        } else if (hours > 0) {
            return hours + " hour" + (hours > 1 ? "s" : "");
        } else if (minutes > 0) {
            return minutes + " minute" + (minutes > 1 ? "s" : "");
        } else {
            return seconds + " second" + (seconds > 1 ? "s" : "");
        }
    }
    
    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1 && sender.hasPermission("frizzlenperms.admin.info")) {
            String partial = args[0].toLowerCase();
            return Bukkit.getOnlinePlayers().stream()
                .map(Player::getName)
                .filter(name -> name.toLowerCase().startsWith(partial))
                .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }
} 