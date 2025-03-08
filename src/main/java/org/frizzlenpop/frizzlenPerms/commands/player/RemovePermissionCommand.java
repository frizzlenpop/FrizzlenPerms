package org.frizzlenpop.frizzlenPerms.commands.player;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.frizzlenpop.frizzlenPerms.FrizzlenPerms;
import org.frizzlenpop.frizzlenPerms.commands.SubCommand;
import org.frizzlenpop.frizzlenPerms.models.PlayerData;
import org.frizzlenpop.frizzlenPerms.utils.MessageUtils;
import org.frizzlenpop.frizzlenPerms.models.AuditLog;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Command to remove a permission from a player.
 */
public class RemovePermissionCommand implements SubCommand {
    
    private final FrizzlenPerms plugin;
    
    /**
     * Creates a new RemovePermissionCommand.
     *
     * @param plugin The plugin instance
     */
    public RemovePermissionCommand(FrizzlenPerms plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public String getName() {
        return "removepermission";
    }
    
    @Override
    public String getDescription() {
        return "Removes a permission from a player.";
    }
    
    @Override
    public String getUsage() {
        return "/frizzlenperms removepermission <player> <permission> [world]";
    }
    
    @Override
    public String getPermission() {
        return "frizzlenperms.admin.removepermission";
    }
    
    @Override
    public int getMinArgs() {
        return 2;
    }
    
    @Override
    public List<String> getAliases() {
        return List.of("removeperm", "rmperm", "delperm", "revoke");
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
        String world = args.length > 2 ? args[2] : null;
        
        // Get player data
        PlayerData playerData = plugin.getDataManager().getPlayerDataByName(playerName);
        if (playerData == null) {
            MessageUtils.sendMessage(sender, "error.player-not-found", Map.of(
                "player", playerName
            ));
            return false;
        }
        
        // Check if player has the permission
        boolean hasPermission = false;
        if (world == null) {
            hasPermission = playerData.getPermissions().contains(permission);
            if (!hasPermission) {
                MessageUtils.sendMessage(sender, "error.doesnt-have-permission", Map.of(
                    "player", playerName,
                    "permission", permission
                ));
                return false;
            }
        } else {
            Map<String, Set<String>> worldPerms = playerData.getWorldPermissions();
            hasPermission = worldPerms.containsKey(world) && worldPerms.get(world).contains(permission);
            if (!hasPermission) {
                MessageUtils.sendMessage(sender, "error.doesnt-have-permission-world", Map.of(
                    "player", playerName,
                    "permission", permission,
                    "world", world
                ));
                return false;
            }
        }
        
        // Get player (might be offline)
        Player player = Bukkit.getPlayer(playerData.getUuid());
        
        // Get the actor who executed the command
        Player actor = null;
        if (sender instanceof Player) {
            actor = (Player) sender;
        }
        
        try {
            // Remove the permission
            if (world == null) {
                // Remove global permission
                playerData.getPermissions().remove(permission);
            } else {
                // Remove world-specific permission
                Map<String, Set<String>> worldPerms = playerData.getWorldPermissions();
                if (worldPerms.containsKey(world)) {
                    worldPerms.get(world).remove(permission);
                    // Remove empty set if no more permissions for this world
                    if (worldPerms.get(world).isEmpty()) {
                        worldPerms.remove(world);
                    }
                }
            }
            
            // Save the changes
            plugin.getDataManager().savePlayerData(playerData);
            
            // Update permissions if player is online
            if (player != null && player.isOnline()) {
                plugin.getPermissionManager().setupPermissions(player);
            }
            
            // Log the action
            plugin.getAuditManager().logAction(
                actor != null ? actor.getUniqueId() : null,
                actor != null ? actor.getName() : "Console",
                AuditLog.ActionType.PERMISSION_REMOVE,
                playerName,
                "Removed permission " + permission + (world != null ? " in world " + world : ""),
                plugin.getConfigManager().getServerName() != null ? plugin.getConfigManager().getServerName() : "default",
                playerData.getUuid()
            );
            
            // Send success message
            if (world == null) {
                MessageUtils.sendMessage(sender, "admin.removeperm-success", Map.of(
                    "player", playerName,
                    "permission", permission
                ));
            } else {
                MessageUtils.sendMessage(sender, "admin.removeperm-success-world", Map.of(
                    "player", playerName,
                    "permission", permission,
                    "world", world
                ));
            }
            
            // Send notification to player if online
            if (player != null && player.isOnline() && !player.equals(sender)) {
                if (world == null) {
                    MessageUtils.sendMessage(player, "player.perm-removed", Map.of(
                        "permission", permission
                    ));
                } else {
                    MessageUtils.sendMessage(player, "player.perm-removed-world", Map.of(
                        "permission", permission,
                        "world", world
                    ));
                }
            }
            
            return true;
        } catch (Exception e) {
            plugin.getLogger().severe("Error removing permission from " + playerName + ": " + e.getMessage());
            e.printStackTrace();
            MessageUtils.sendMessage(sender, "error.remove-perm-failed", Map.of(
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
            // Suggest player's permissions
            String partial = args[1].toLowerCase();
            
            if (args[0].length() > 0) {
                PlayerData playerData = plugin.getDataManager().getPlayerDataByName(args[0]);
                if (playerData != null) {
                    return playerData.getPermissions().stream()
                        .filter(perm -> perm.toLowerCase().startsWith(partial))
                        .collect(Collectors.toList());
                }
            }
            
            // Fallback to common permissions
            List<String> commonPerms = Arrays.asList(
                "frizzlenperms.player.",
                "frizzlenperms.admin.",
                "minecraft.command."
            );
            
            return commonPerms.stream()
                .filter(perm -> perm.toLowerCase().startsWith(partial))
                .collect(Collectors.toList());
        } else if (args.length == 3) {
            // Suggest world names
            String partial = args[2].toLowerCase();
            
            if (args[0].length() > 0) {
                PlayerData playerData = plugin.getDataManager().getPlayerDataByName(args[0]);
                if (playerData != null) {
                    // Suggest worlds where the player has permissions
                    return playerData.getWorldPermissions().keySet().stream()
                        .filter(world -> world.toLowerCase().startsWith(partial))
                        .collect(Collectors.toList());
                }
            }
            
            return Bukkit.getWorlds().stream()
                .map(world -> world.getName())
                .filter(name -> name.toLowerCase().startsWith(partial))
                .collect(Collectors.toList());
        }
        
        return Collections.emptyList();
    }
} 