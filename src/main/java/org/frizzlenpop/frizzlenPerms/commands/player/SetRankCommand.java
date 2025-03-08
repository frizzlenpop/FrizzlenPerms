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
 * Command to set a player's primary rank.
 */
public class SetRankCommand implements SubCommand {
    
    private final FrizzlenPerms plugin;
    
    /**
     * Creates a new SetRankCommand.
     *
     * @param plugin The plugin instance
     */
    public SetRankCommand(FrizzlenPerms plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public String getName() {
        return "setrank";
    }
    
    @Override
    public String getDescription() {
        return "Sets a player's primary rank.";
    }
    
    @Override
    public String getUsage() {
        return "/frizzlenperms setrank <player> <rank>";
    }
    
    @Override
    public String getPermission() {
        return "frizzlenperms.admin.setrank";
    }
    
    @Override
    public int getMinArgs() {
        return 2;
    }
    
    @Override
    public List<String> getAliases() {
        return List.of("set", "primaryrank");
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
        
        // Get player (might be offline)
        Player player = Bukkit.getPlayer(playerData.getUuid());
        
        // Get the actor who executed the command
        Player actor = null;
        if (sender instanceof Player) {
            actor = (Player) sender;
        }
        
        // Set the primary rank
        try {
            String oldRank = playerData.getPrimaryRank();
            plugin.getRankManager().setPrimaryRank(playerData.getUuid(), rank.getName(), actor);
            
            // Send success message
            MessageUtils.sendMessage(sender, "admin.setrank-success", Map.of(
                "player", playerName,
                "rank", rank.getDisplayName(),
                "old_rank", oldRank != null ? plugin.getRankManager().getRank(oldRank).getDisplayName() : "None"
            ));
            
            // Send notification to player if online
            if (player != null && player.isOnline() && !player.equals(sender)) {
                MessageUtils.sendMessage(player, "player.rank-changed", Map.of(
                    "rank", rank.getDisplayName()
                ));
            }
            
            return true;
        } catch (Exception e) {
            plugin.getLogger().severe("Error setting rank for " + playerName + ": " + e.getMessage());
            e.printStackTrace();
            MessageUtils.sendMessage(sender, "error.set-rank-failed", Map.of(
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
            return plugin.getRankManager().getAllRankNames().stream()
                .filter(name -> name.toLowerCase().startsWith(partial))
                .collect(Collectors.toList());
        }
        
        return Collections.emptyList();
    }
} 