package org.frizzlenpop.frizzlenPerms.discord;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.frizzlenpop.frizzlenPerms.FrizzlenPerms;
import org.frizzlenpop.frizzlenPerms.models.PlayerData;
import org.frizzlenpop.frizzlenPerms.utils.MessageUtils;

import java.util.UUID;
import java.util.logging.Level;

/**
 * Handles Discord events.
 */
public class DiscordListener extends ListenerAdapter {
    
    private final FrizzlenPerms plugin;
    
    /**
     * Creates a new DiscordListener.
     *
     * @param plugin The plugin instance
     */
    public DiscordListener(FrizzlenPerms plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Called when JDA is ready.
     *
     * @param event The event
     */
    @Override
    public void onReady(ReadyEvent event) {
        // Register slash commands
        event.getJDA().updateCommands().addCommands(
            Commands.slash("link", "Link your Discord account to your Minecraft account")
                .addOption(OptionType.STRING, "code", "The link code from the game", true),
            Commands.slash("sync", "Sync your Discord roles with your Minecraft ranks")
        ).queue();
        
        plugin.getLogger().info("Discord slash commands registered.");
    }
    
    /**
     * Called when a slash command is used.
     *
     * @param event The event
     */
    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        // Check if the command is from the guild
        if (plugin.getDiscordManager().getGuild() != null && 
            !event.getGuild().getId().equals(plugin.getDiscordManager().getGuild().getId())) {
            return;
        }
        
        // Handle commands
        switch (event.getName()) {
            case "link":
                handleLinkCommand(event);
                break;
            case "sync":
                handleSyncCommand(event);
                break;
        }
    }
    
    /**
     * Handles the link command.
     *
     * @param event The event
     */
    private void handleLinkCommand(SlashCommandInteractionEvent event) {
        // Defer reply
        event.deferReply(true).queue();
        
        // Get code
        String code = event.getOption("code").getAsString();
        
        // Get player UUID from code
        UUID playerUuid = plugin.getSyncManager().getPlayerUuidFromCode(code);
        if (playerUuid == null) {
            event.getHook().sendMessage("Invalid or expired link code.").queue();
            return;
        }
        
        // Get player data
        PlayerData playerData = plugin.getDataManager().getPlayerData(playerUuid);
        if (playerData == null) {
            event.getHook().sendMessage("Player data not found.").queue();
            return;
        }
        
        // Check if already linked
        if (playerData.getDiscordId() != null && !playerData.getDiscordId().isEmpty()) {
            event.getHook().sendMessage("Your Minecraft account is already linked to a Discord account.").queue();
            return;
        }
        
        // Set Discord ID
        playerData.setDiscordId(event.getUser().getId());
        
        // Save player data
        plugin.getDataManager().savePlayerData(playerData);
        
        // Sync player roles
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            plugin.getDiscordManager().syncPlayerRoles(playerUuid);
        });
        
        // Send success message
        event.getHook().sendMessage("Successfully linked to Minecraft account: " + playerData.getPlayerName()).queue();
        
        // Send message to player if online
        Player player = Bukkit.getPlayer(playerUuid);
        if (player != null && player.isOnline()) {
            player.sendMessage(MessageUtils.formatMessage("&aYour account has been linked to Discord user: &e" + 
                event.getUser().getName() + "#" + event.getUser().getDiscriminator()));
        }
        
        // Log the link
        plugin.getLogger().info("Player " + playerData.getPlayerName() + " linked to Discord user: " + 
            event.getUser().getName() + "#" + event.getUser().getDiscriminator());
    }
    
    /**
     * Handles the sync command.
     *
     * @param event The event
     */
    private void handleSyncCommand(SlashCommandInteractionEvent event) {
        // Defer reply
        event.deferReply(true).queue();
        
        // Find player data with this Discord ID
        String discordId = event.getUser().getId();
        PlayerData playerData = null;
        
        for (PlayerData pd : plugin.getDataManager().getAllPlayerData()) {
            if (discordId.equals(pd.getDiscordId())) {
                playerData = pd;
                break;
            }
        }
        
        // Check if linked
        if (playerData == null) {
            event.getHook().sendMessage("Your Discord account is not linked to any Minecraft account. " +
                "Use `/link` in-game to get a link code.").queue();
            return;
        }
        
        // Sync player roles
        try {
            boolean success = plugin.getDiscordManager().syncPlayerRoles(playerData.getUuid());
            
            if (success) {
                event.getHook().sendMessage("Successfully synced your Discord roles with your Minecraft ranks.").queue();
            } else {
                event.getHook().sendMessage("Failed to sync your Discord roles. Please try again later.").queue();
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error syncing Discord roles", e);
            event.getHook().sendMessage("An error occurred while syncing your Discord roles. Please try again later.").queue();
        }
    }
} 