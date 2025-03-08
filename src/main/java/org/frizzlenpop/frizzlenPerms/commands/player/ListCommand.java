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
 * Command to list players with a specific rank.
 */
public class ListCommand implements SubCommand {
    
    private final FrizzlenPerms plugin;
    
    /**
     * Creates a new ListCommand.
     *
     * @param plugin The plugin instance
     */
    public ListCommand(FrizzlenPerms plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public String getName() {
        return "list";
    }
    
    @Override
    public String getDescription() {
        return "Lists players with a specific rank.";
    }
    
    @Override
    public String getUsage() {
        return "/frizzlenperms list [rank]";
    }
    
    @Override
    public String getPermission() {
        return "frizzlenperms.player.list";
    }
    
    @Override
    public int getMinArgs() {
        return 0;
    }
    
    @Override
    public List<String> getAliases() {
        return List.of("players", "who");
    }
    
    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (args.length == 0) {
            // No rank specified, list all ranks with player counts
            listAllRanks(sender);
            return true;
        }
        
        // Get the specified rank
        String rankName = args[0];
        Rank rank = plugin.getRankManager().getRank(rankName);
        
        if (rank == null) {
            MessageUtils.sendMessage(sender, "error.rank-not-found", Map.of("rank", rankName));
            return true;
        }
        
        // Get all players with this rank
        List<PlayerData> allPlayers = plugin.getDataManager().getAllPlayerData();
        List<PlayerData> playersWithRank = new ArrayList<>();
        
        for (PlayerData playerData : allPlayers) {
            if (rankName.equals(playerData.getPrimaryRank()) || 
                playerData.getSecondaryRanks().contains(rankName)) {
                playersWithRank.add(playerData);
            }
        }
        
        // Sort by name
        playersWithRank.sort(Comparator.comparing(PlayerData::getPlayerName));
        
        // Display results
        MessageUtils.sendMessage(sender, "player.list-header", Map.of(
            "rank", rank.getDisplayName(),
            "count", String.valueOf(playersWithRank.size())
        ));
        
        if (playersWithRank.isEmpty()) {
            MessageUtils.sendMessage(sender, "player.list-empty");
            return true;
        }
        
        // Split into online and offline players
        List<String> onlinePlayers = new ArrayList<>();
        List<String> offlinePlayers = new ArrayList<>();
        
        for (PlayerData playerData : playersWithRank) {
            String playerName = playerData.getPlayerName();
            Player player = Bukkit.getPlayer(playerData.getUuid());
            
            if (player != null && player.isOnline()) {
                onlinePlayers.add(playerName);
            } else {
                offlinePlayers.add(playerName);
            }
        }
        
        // Show online players
        if (!onlinePlayers.isEmpty()) {
            MessageUtils.sendMessage(sender, "player.list-online-header", Map.of(
                "count", String.valueOf(onlinePlayers.size())
            ));
            
            String onlineList = String.join(", ", onlinePlayers);
            MessageUtils.sendMessage(sender, "player.list-players", Map.of(
                "players", onlineList
            ));
        }
        
        // Show offline players if admin
        if (!offlinePlayers.isEmpty() && sender.hasPermission("frizzlenperms.admin.list")) {
            MessageUtils.sendMessage(sender, "player.list-offline-header", Map.of(
                "count", String.valueOf(offlinePlayers.size())
            ));
            
            String offlineList = String.join(", ", offlinePlayers);
            MessageUtils.sendMessage(sender, "player.list-players", Map.of(
                "players", offlineList
            ));
        }
        
        return true;
    }
    
    /**
     * Lists all ranks with player counts.
     *
     * @param sender The command sender
     */
    private void listAllRanks(CommandSender sender) {
        List<Rank> ranks = plugin.getRankManager().getAllRanks();
        List<PlayerData> allPlayers = plugin.getDataManager().getAllPlayerData();
        
        MessageUtils.sendMessage(sender, "player.list-all-ranks-header");
        
        for (Rank rank : ranks) {
            int count = 0;
            int online = 0;
            
            for (PlayerData playerData : allPlayers) {
                boolean hasRank = rank.getName().equals(playerData.getPrimaryRank()) || 
                                 playerData.getSecondaryRanks().contains(rank.getName());
                
                if (hasRank) {
                    count++;
                    
                    Player player = Bukkit.getPlayer(playerData.getUuid());
                    if (player != null && player.isOnline()) {
                        online++;
                    }
                }
            }
            
            MessageUtils.sendMessage(sender, "player.list-rank-entry", Map.of(
                "rank", rank.getDisplayName(),
                "count", String.valueOf(count),
                "online", String.valueOf(online)
            ));
        }
    }
    
    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            String partial = args[0].toLowerCase();
            return plugin.getRankManager().getAllRankNames().stream()
                .filter(name -> name.toLowerCase().startsWith(partial))
                .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }
} 