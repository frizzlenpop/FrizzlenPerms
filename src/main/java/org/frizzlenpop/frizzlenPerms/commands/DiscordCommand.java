package org.frizzlenpop.frizzlenPerms.commands;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.frizzlenpop.frizzlenPerms.FrizzlenPerms;
import org.frizzlenpop.frizzlenPerms.models.PlayerData;
import org.frizzlenpop.frizzlenPerms.utils.MessageUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Command for Discord integration.
 */
public class DiscordCommand implements SubCommand {
    
    private final FrizzlenPerms plugin;
    
    /**
     * Creates a new DiscordCommand.
     *
     * @param plugin The plugin instance
     */
    public DiscordCommand(FrizzlenPerms plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public String getName() {
        return "discord";
    }
    
    @Override
    public String getDescription() {
        return "Manage Discord integration";
    }
    
    @Override
    public String getUsage() {
        return "/fp discord <link|unlink|sync>";
    }
    
    @Override
    public String getPermission() {
        return "frizzlenperms.discord";
    }
    
    @Override
    public int getMinArgs() {
        return 1;
    }
    
    @Override
    public List<String> getAliases() {
        return Collections.emptyList();
    }
    
    @Override
    public boolean isPlayerOnly() {
        return true;
    }
    
    @Override
    public boolean execute(CommandSender sender, String[] args) {
        // Check if Discord is enabled
        if (!plugin.getConfigManager().isDiscordEnabled()) {
            MessageUtils.sendMessage(sender, "discord.disabled");
            return true;
        }
        
        // Get player
        Player player = (Player) sender;
        
        // Handle subcommands
        if (args.length < 1) {
            MessageUtils.sendMessage(sender, "discord.usage");
            return false;
        }
        
        String subCommand = args[0].toLowerCase();
        
        switch (subCommand) {
            case "link":
                handleLink(player);
                break;
            case "unlink":
                handleUnlink(player);
                break;
            case "sync":
                handleSync(player);
                break;
            default:
                MessageUtils.sendMessage(sender, "discord.usage");
                return false;
        }
        
        return true;
    }
    
    /**
     * Handles the link subcommand.
     *
     * @param player The player
     */
    private void handleLink(Player player) {
        // Get player data
        PlayerData playerData = plugin.getDataManager().getPlayerData(player.getUniqueId());
        
        // Check if already linked
        if (playerData.getDiscordId() != null && !playerData.getDiscordId().isEmpty()) {
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("discord_id", playerData.getDiscordId());
            MessageUtils.sendMessage(player, "discord.already_linked", placeholders);
            return;
        }
        
        // Generate link code
        plugin.getSyncManager().generateLinkCode(player);
    }
    
    /**
     * Handles the unlink subcommand.
     *
     * @param player The player
     */
    private void handleUnlink(Player player) {
        // Get player data
        PlayerData playerData = plugin.getDataManager().getPlayerData(player.getUniqueId());
        
        // Check if linked
        if (playerData.getDiscordId() == null || playerData.getDiscordId().isEmpty()) {
            MessageUtils.sendMessage(player, "discord.not_linked");
            return;
        }
        
        // Unlink player
        boolean success = plugin.getDiscordManager().unlinkPlayer(player);
        
        if (success) {
            MessageUtils.sendMessage(player, "discord.unlinked");
        } else {
            MessageUtils.sendMessage(player, "discord.unlink_failed");
        }
    }
    
    /**
     * Handles the sync subcommand.
     *
     * @param player The player
     */
    private void handleSync(Player player) {
        // Get player data
        PlayerData playerData = plugin.getDataManager().getPlayerData(player.getUniqueId());
        
        // Check if linked
        if (playerData.getDiscordId() == null || playerData.getDiscordId().isEmpty()) {
            MessageUtils.sendMessage(player, "discord.not_linked");
            return;
        }
        
        // Sync player
        boolean success = plugin.getSyncManager().syncPlayerWithDiscord(player);
        
        if (success) {
            MessageUtils.sendMessage(player, "discord.synced");
        } else {
            MessageUtils.sendMessage(player, "discord.sync_failed");
        }
    }
    
    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return Arrays.asList("link", "unlink", "sync");
        }
        return Collections.emptyList();
    }
} 