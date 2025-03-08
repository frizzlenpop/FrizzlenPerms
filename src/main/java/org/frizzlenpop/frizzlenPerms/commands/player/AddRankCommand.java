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
import java.util.stream.Collectors;

/**
 * Command to add a secondary rank to a player.
 */
public class AddRankCommand implements SubCommand {
    
    private final FrizzlenPerms plugin;
    
    /**
     * Creates a new AddRankCommand.
     *
     * @param plugin The plugin instance
     */
    public AddRankCommand(FrizzlenPerms plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public String getName() {
        return "addrank";
    }
    
    @Override
    public String getDescription() {
        return "Adds a secondary rank to a player.";
    }
    
    @Override
    public String getUsage() {
        return "/frizzlenperms addrank <player> <rank>";
    }
    
    @Override
    public String getPermission() {
        return "frizzlenperms.admin.addrank";
    }
    
    @Override
    public int getMinArgs() {
        return 2;
    }
    
    @Override
    public List<String> getAliases() {
        return List.of("addsecondary", "secondaryrank");
    }
    
    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (args.length < 2) {
            MessageUtils.sendMessage(sender, "error.missing-arguments", Map.of(
                "usage", getUsage()
            ));
            return false;
        }
        
        String playerName = args[0];
        String rankName = args[1];
        
        // Get player data
        PlayerData playerData = plugin.getDataManager().getPlayerDataByName(playerName);
        if (playerData == null) {
            MessageUtils.sendMessage(sender, "error.player-not-found", Map.of(
                "player", playerName
            ));
            return false;
        }
        
        // Get rank
        Rank rank = plugin.getRankManager().getRank(rankName);
        if (rank == null) {
            MessageUtils.sendMessage(sender, "error.rank-not-found", Map.of(
                "rank", rankName
            ));
            return false;
        }
        
        // Check if player already has this rank
        if (playerData.getSecondaryRanks().contains(rank.getName())) {
            MessageUtils.sendMessage(sender, "error.already-has-rank", Map.of(
                "player", playerName,
                "rank", rank.getDisplayName()
            ));
            return false;
        }
        
        // Get player (might be offline)
        Player player = Bukkit.getPlayer(playerData.getUuid());
        
        // Get the actor who executed the command
        Player actor = null;
        if (sender instanceof Player) {
            actor = (Player) sender;
        }
        
        // Add the secondary rank
        try {
            // Add the rank to the player's data
            playerData.getSecondaryRanks().add(rank.getName());
            plugin.getDataManager().savePlayerData(playerData);
            
            // Update permissions if player is online
            if (player != null && player.isOnline()) {
                plugin.getPermissionManager().setupPermissions(player);
            }
            
            // Log the action
            plugin.getAuditManager().logAction(
                actor != null ? actor.getUniqueId() : null,
                actor != null ? actor.getName() : "Console",
                org.frizzlenpop.frizzlenPerms.models.AuditLog.ActionType.PLAYER_RANK_ADD,
                playerName,
                "Added secondary rank " + rank.getName(),
                plugin.getConfigManager().getServerName(),
                playerData.getUuid()
            );
            
            // Send success message
            MessageUtils.sendMessage(sender, "admin.addrank-success", Map.of(
                "player", playerName,
                "rank", rank.getDisplayName()
            ));
            
            // Send notification to player if online
            if (player != null && player.isOnline() && !player.equals(sender)) {
                MessageUtils.sendMessage(player, "player.rank-added", Map.of(
                    "rank", rank.getDisplayName()
                ));
            }
            
            return true;
        } catch (Exception e) {
            plugin.getLogger().severe("Error adding rank for " + playerName + ": " + e.getMessage());
            e.printStackTrace();
            MessageUtils.sendMessage(sender, "error.add-rank-failed", Map.of(
                "error", e.getMessage()
            ));
            return false;
        }
    }
    
    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            // Suggest player names
            String partial = args[0].toLowerCase();
            return Bukkit.getOnlinePlayers().stream()
                .map(Player::getName)
                .filter(name -> name.toLowerCase().startsWith(partial))
                .collect(Collectors.toList());
        } else if (args.length == 2) {
            // Suggest rank names
            String partial = args[1].toLowerCase();
            
            // If a player is specified, only suggest ranks they don't already have
            if (args[0].length() > 0) {
                PlayerData playerData = plugin.getDataManager().getPlayerDataByName(args[0]);
                if (playerData != null) {
                    Set<String> currentRanks = new HashSet<>(playerData.getSecondaryRanks());
                    if (playerData.getPrimaryRank() != null) {
                        currentRanks.add(playerData.getPrimaryRank());
                    }
                    
                    return plugin.getRankManager().getAllRankNames().stream()
                        .filter(name -> !currentRanks.contains(name))
                        .filter(name -> name.toLowerCase().startsWith(partial))
                        .collect(Collectors.toList());
                }
            }
            
            return plugin.getRankManager().getAllRankNames().stream()
                .filter(name -> name.toLowerCase().startsWith(partial))
                .collect(Collectors.toList());
        }
        
        return Collections.emptyList();
    }
} 