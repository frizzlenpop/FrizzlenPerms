package org.frizzlenpop.frizzlenPerms.commands.player;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.frizzlenpop.frizzlenPerms.FrizzlenPerms;
import org.frizzlenpop.frizzlenPerms.commands.SubCommand;
import org.frizzlenpop.frizzlenPerms.models.PlayerData;
import org.frizzlenpop.frizzlenPerms.utils.MessageUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Command to check if a player has a specific permission.
 */
public class CheckPermissionCommand implements SubCommand {
    
    private final FrizzlenPerms plugin;
    
    /**
     * Creates a new CheckPermissionCommand.
     *
     * @param plugin The plugin instance
     */
    public CheckPermissionCommand(FrizzlenPerms plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public String getName() {
        return "checkpermission";
    }
    
    @Override
    public String getDescription() {
        return "Checks if a player has a specific permission.";
    }
    
    @Override
    public String getUsage() {
        return "/frizzlenperms checkpermission <player> <permission>";
    }
    
    @Override
    public String getPermission() {
        return "frizzlenperms.admin.checkpermission";
    }
    
    @Override
    public int getMinArgs() {
        return 2;
    }
    
    @Override
    public List<String> getAliases() {
        return List.of("check", "hasperm", "has");
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
        String permission = args[1];
        
        // Check if player is online
        Player targetPlayer = Bukkit.getPlayer(playerName);
        if (targetPlayer == null) {
            // Try to check in the player data (offline)
            PlayerData playerData = plugin.getDataManager().getPlayerDataByName(playerName);
            if (playerData == null) {
                MessageUtils.sendMessage(sender, "error.player-not-found", Map.of(
                    "player", playerName
                ));
                return false;
            }
            
            // Check directly in player data (rough estimate, not accounting for inheritance)
            boolean hasDirectPerm = playerData.getPermissions().contains(permission);
            boolean hasNegatedPerm = playerData.getPermissions().contains("-" + permission);
            
            if (hasNegatedPerm) {
                MessageUtils.sendMessage(sender, "admin.checkperm-offline-negated", Map.of(
                    "player", playerName,
                    "permission", permission
                ));
            } else if (hasDirectPerm) {
                MessageUtils.sendMessage(sender, "admin.checkperm-offline-has", Map.of(
                    "player", playerName,
                    "permission", permission
                ));
            } else {
                // Check if any of the player's ranks grant this permission
                boolean hasRankPerm = false;
                String primaryRank = playerData.getPrimaryRank();
                
                // Check primary rank
                if (primaryRank != null) {
                    hasRankPerm = checkRankPermission(primaryRank, permission);
                }
                
                // Check secondary ranks if needed
                if (!hasRankPerm) {
                    for (String rankName : playerData.getSecondaryRanks()) {
                        if (checkRankPermission(rankName, permission)) {
                            hasRankPerm = true;
                            break;
                        }
                    }
                }
                
                if (hasRankPerm) {
                    MessageUtils.sendMessage(sender, "admin.checkperm-offline-has-rank", Map.of(
                        "player", playerName,
                        "permission", permission
                    ));
                } else {
                    MessageUtils.sendMessage(sender, "admin.checkperm-offline-doesnt-have", Map.of(
                        "player", playerName,
                        "permission", permission
                    ));
                }
            }
            
            return true;
        }
        
        // Player is online, check actual permission
        boolean hasPermission = targetPlayer.hasPermission(permission);
        
        if (hasPermission) {
            MessageUtils.sendMessage(sender, "players.check-permission-true", Map.of(
                "player", playerName,
                "permission", permission
            ));
        } else {
            MessageUtils.sendMessage(sender, "players.check-permission-false", Map.of(
                "player", playerName,
                "permission", permission
            ));
        }
        
        return true;
    }
    
    /**
     * Checks if a rank grants a permission.
     *
     * @param rankName The rank name
     * @param permission The permission to check
     * @return True if the rank grants the permission, false otherwise
     */
    private boolean checkRankPermission(String rankName, String permission) {
        // Get the rank
        org.frizzlenpop.frizzlenPerms.models.Rank rank = plugin.getRankManager().getRank(rankName);
        if (rank == null) {
            return false;
        }
        
        // Check if the rank grants the permission
        if (rank.getPermissions().contains(permission)) {
            return true;
        }
        
        // Check for wildcard permissions
        String permBase = permission;
        while (permBase.contains(".")) {
            permBase = permBase.substring(0, permBase.lastIndexOf('.'));
            if (rank.getPermissions().contains(permBase + ".*")) {
                return true;
            }
        }
        
        // Check inherited ranks (recursive)
        for (String inheritedRank : rank.getInheritance()) {
            if (!inheritedRank.equals(rankName) && checkRankPermission(inheritedRank, permission)) {
                return true;
            }
        }
        
        return false;
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
            // Suggest some common permissions
            String partial = args[1].toLowerCase();
            List<String> commonPerms = Arrays.asList(
                "frizzlenperms.player.",
                "frizzlenperms.admin.",
                "minecraft.command."
            );
            
            return commonPerms.stream()
                .filter(perm -> perm.toLowerCase().startsWith(partial))
                .collect(Collectors.toList());
        }
        
        return Collections.emptyList();
    }
} 