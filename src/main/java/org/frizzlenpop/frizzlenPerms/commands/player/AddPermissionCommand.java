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
 * Command to add a permission to a player.
 */
public class AddPermissionCommand implements SubCommand {
    
    private final FrizzlenPerms plugin;
    
    /**
     * Creates a new AddPermissionCommand.
     *
     * @param plugin The plugin instance
     */
    public AddPermissionCommand(FrizzlenPerms plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public String getName() {
        return "addpermission";
    }
    
    @Override
    public String getDescription() {
        return "Adds a permission to a player.";
    }
    
    @Override
    public String getUsage() {
        return "/frizzlenperms addpermission <player> <permission> [world]";
    }
    
    @Override
    public String getPermission() {
        return "frizzlenperms.admin.addpermission";
    }
    
    @Override
    public int getMinArgs() {
        return 2;
    }
    
    @Override
    public List<String> getAliases() {
        return List.of("addperm", "grant");
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
        
        // Check if player already has the permission
        if (world == null) {
            if (playerData.getPermissions().contains(permission)) {
                MessageUtils.sendMessage(sender, "error.already-has-permission", Map.of(
                    "player", playerName,
                    "permission", permission
                ));
                return false;
            }
        } else {
            Map<String, Set<String>> worldPerms = playerData.getWorldPermissions();
            if (worldPerms.containsKey(world) && worldPerms.get(world).contains(permission)) {
                MessageUtils.sendMessage(sender, "error.already-has-permission-world", Map.of(
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
            // Add the permission
            if (world == null) {
                // Add global permission
                playerData.getPermissions().add(permission);
            } else {
                // Add world-specific permission
                Map<String, Set<String>> worldPerms = playerData.getWorldPermissions();
                Set<String> perms = worldPerms.computeIfAbsent(world, k -> new HashSet<>());
                perms.add(permission);
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
                "PLAYER_PERMISSION_ADD", // Using string instead of enum
                playerName,
                "Added permission " + permission + (world != null ? " in world " + world : ""),
                "default", // Server name
                playerData.getUuid()
            );
            
            // Send success message
            if (world == null) {
                MessageUtils.sendMessage(sender, "admin.addperm-success", Map.of(
                    "player", playerName,
                    "permission", permission
                ));
            } else {
                MessageUtils.sendMessage(sender, "admin.addperm-success-world", Map.of(
                    "player", playerName,
                    "permission", permission,
                    "world", world
                ));
            }
            
            // Send notification to player if online
            if (player != null && player.isOnline() && !player.equals(sender)) {
                if (world == null) {
                    MessageUtils.sendMessage(player, "player.perm-added", Map.of(
                        "permission", permission
                    ));
                } else {
                    MessageUtils.sendMessage(player, "player.perm-added-world", Map.of(
                        "permission", permission,
                        "world", world
                    ));
                }
            }
            
            return true;
        } catch (Exception e) {
            plugin.getLogger().severe("Error adding permission for " + playerName + ": " + e.getMessage());
            e.printStackTrace();
            MessageUtils.sendMessage(sender, "error.add-perm-failed", Map.of(
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
        } else if (args.length == 3) {
            // Suggest world names
            String partial = args[2].toLowerCase();
            return Bukkit.getWorlds().stream()
                .map(world -> world.getName())
                .filter(name -> name.toLowerCase().startsWith(partial))
                .collect(Collectors.toList());
        }
        
        return Collections.emptyList();
    }
} 