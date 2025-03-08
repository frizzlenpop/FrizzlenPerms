package org.frizzlenpop.frizzlenPerms.discord;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.requests.GatewayIntent;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.frizzlenpop.frizzlenPerms.FrizzlenPerms;
import org.frizzlenpop.frizzlenPerms.data.DataManager;
import org.frizzlenpop.frizzlenPerms.models.AuditLog;
import org.frizzlenpop.frizzlenPerms.models.PlayerData;

import java.awt.Color;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

/**
 * Manages Discord integration.
 */
public class DiscordManager {
    
    private final FrizzlenPerms plugin;
    private JDA jda;
    private Guild guild;
    private TextChannel logChannel;
    private final Map<String, String> roleMappings;
    private BukkitTask syncTask;
    
    /**
     * Creates a new DiscordManager.
     *
     * @param plugin The plugin instance
     */
    public DiscordManager(FrizzlenPerms plugin) {
        this.plugin = plugin;
        this.roleMappings = new HashMap<>();
    }
    
    /**
     * Initializes the Discord connection.
     */
    public void initialize() {
        // Check if Discord is enabled
        if (!plugin.getConfigManager().isDiscordEnabled()) {
            plugin.getLogger().info("Discord integration is disabled.");
            return;
        }
        
        // Get Discord token
        String token = plugin.getConfigManager().getDiscordToken();
        if (token == null || token.isEmpty()) {
            plugin.getLogger().warning("Discord token is not set. Discord integration will be disabled.");
            return;
        }
        
        try {
            // Build JDA instance
            jda = JDABuilder.createDefault(token)
                .enableIntents(GatewayIntent.GUILD_MEMBERS)
                .addEventListeners(new DiscordListener(plugin))
                .build();
            
            // Wait for JDA to be ready
            jda.awaitReady();
            
            // Get guild
            String guildId = plugin.getConfigManager().getDiscordGuildId();
            if (guildId == null || guildId.isEmpty()) {
                plugin.getLogger().warning("Discord guild ID is not set. Discord integration will be limited.");
            } else {
                guild = jda.getGuildById(guildId);
                if (guild == null) {
                    plugin.getLogger().warning("Could not find Discord guild with ID: " + guildId);
                } else {
                    plugin.getLogger().info("Connected to Discord guild: " + guild.getName());
                }
            }
            
            // Get log channel
            String logChannelId = plugin.getConfigManager().getDiscordLogChannelId();
            if (logChannelId != null && !logChannelId.isEmpty() && guild != null) {
                logChannel = guild.getTextChannelById(logChannelId);
                if (logChannel == null) {
                    plugin.getLogger().warning("Could not find Discord log channel with ID: " + logChannelId);
                } else {
                    plugin.getLogger().info("Connected to Discord log channel: " + logChannel.getName());
                }
            }
            
            // Load role mappings
            loadRoleMappings();
            
            // Start sync task
            startSyncTask();
            
            plugin.getLogger().info("Discord integration initialized successfully.");
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error initializing Discord integration", e);
        }
    }
    
    /**
     * Loads role mappings from the configuration.
     */
    private void loadRoleMappings() {
        roleMappings.clear();
        
        ConfigurationSection mappings = plugin.getConfigManager().getDiscordRoleMappings();
        if (mappings == null) {
            return;
        }
        
        for (String rankName : mappings.getKeys(false)) {
            String roleId = mappings.getString(rankName);
            if (roleId != null && !roleId.isEmpty()) {
                roleMappings.put(rankName.toLowerCase(), roleId);
            }
        }
        
        plugin.getLogger().info("Loaded " + roleMappings.size() + " Discord role mappings.");
    }
    
    /**
     * Starts the sync task.
     */
    private void startSyncTask() {
        // Cancel existing task
        if (syncTask != null) {
            syncTask.cancel();
        }
        
        // Start new task
        int interval = plugin.getConfigManager().getDiscordSyncInterval();
        if (interval <= 0) {
            return;
        }
        
        syncTask = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, this::syncAllPlayers, 20 * 60, 20 * 60 * interval);
        plugin.getLogger().info("Discord sync task started with interval: " + interval + " minutes.");
    }
    
    /**
     * Shuts down the Discord connection.
     */
    public void shutdown() {
        // Cancel sync task
        if (syncTask != null) {
            syncTask.cancel();
            syncTask = null;
        }
        
        // Shutdown JDA
        if (jda != null) {
            jda.shutdown();
            jda = null;
            guild = null;
            logChannel = null;
        }
    }
    
    /**
     * Reloads the Discord integration.
     */
    public void reload() {
        shutdown();
        initialize();
    }
    
    /**
     * Links a player to a Discord user.
     *
     * @param player The player
     * @param discordId The Discord user ID
     * @return True if the link was successful
     */
    public boolean linkPlayer(Player player, String discordId) {
        // Check if Discord is enabled
        if (jda == null || guild == null) {
            return false;
        }
        
        try {
            // Get Discord user
            User discordUser = jda.retrieveUserById(discordId).complete();
            if (discordUser == null) {
                return false;
            }
            
            // Get player data
            PlayerData playerData = plugin.getDataManager().getPlayerData(player.getUniqueId());
            if (playerData == null) {
                return false;
            }
            
            // Set Discord ID
            playerData.setDiscordId(discordId);
            
            // Save player data
            plugin.getDataManager().savePlayerData(playerData);
            
            // Sync player roles
            syncPlayerRoles(player.getUniqueId());
            
            // Log the link
            plugin.getAuditManager().logAction(
                player.getUniqueId(),
                player.getName(),
                AuditLog.ActionType.DISCORD_LINK,
                "Linked to Discord user: " + discordUser.getName() + "#" + discordUser.getDiscriminator(),
                "",
                ""
            );
            
            return true;
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error linking player to Discord", e);
            return false;
        }
    }
    
    /**
     * Unlinks a player from Discord.
     *
     * @param player The player
     * @return True if the unlink was successful
     */
    public boolean unlinkPlayer(Player player) {
        // Get player data
        PlayerData playerData = plugin.getDataManager().getPlayerData(player.getUniqueId());
        if (playerData == null) {
            return false;
        }
        
        // Check if player is linked
        String discordId = playerData.getDiscordId();
        if (discordId == null || discordId.isEmpty()) {
            return false;
        }
        
        // Clear Discord ID
        playerData.setDiscordId(null);
        
        // Save player data
        plugin.getDataManager().savePlayerData(playerData);
        
        // Log the unlink
        plugin.getAuditManager().logAction(
            player.getUniqueId(),
            player.getName(),
            AuditLog.ActionType.DISCORD_UNLINK,
            "Unlinked from Discord",
            "",
            ""
        );
        
        return true;
    }
    
    /**
     * Syncs all players' roles with Discord.
     */
    public void syncAllPlayers() {
        // Check if Discord is enabled
        if (jda == null || guild == null) {
            return;
        }
        
        // Get all player data
        DataManager dataManager = plugin.getDataManager();
        List<PlayerData> allPlayerData = dataManager.getAllPlayerData();
        
        // Sync each player
        for (PlayerData playerData : allPlayerData) {
            String discordId = playerData.getDiscordId();
            if (discordId != null && !discordId.isEmpty()) {
                syncPlayerRoles(playerData.getUuid());
            }
        }
    }
    
    /**
     * Syncs a player's roles with Discord.
     *
     * @param playerUuid The player UUID
     * @return True if the sync was successful
     */
    public boolean syncPlayerRoles(UUID playerUuid) {
        // Check if Discord is enabled
        if (jda == null || guild == null) {
            return false;
        }
        
        try {
            // Get player data
            PlayerData playerData = plugin.getDataManager().getPlayerData(playerUuid);
            if (playerData == null) {
                return false;
            }
            
            // Check if player is linked
            String discordId = playerData.getDiscordId();
            if (discordId == null || discordId.isEmpty()) {
                return false;
            }
            
            // Get Discord member
            Member member = guild.retrieveMemberById(discordId).complete();
            if (member == null) {
                return false;
            }
            
            // Get player ranks
            List<String> primaryRank = new ArrayList<>();
            if (playerData.getPrimaryRank() != null) {
                primaryRank.add(playerData.getPrimaryRank());
            }
            List<String> secondaryRanks = playerData.getSecondaryRanks();
            List<String> allRanks = new ArrayList<>(primaryRank);
            allRanks.addAll(secondaryRanks);
            
            // Get Discord roles to add
            List<Role> rolesToAdd = new ArrayList<>();
            for (String rankName : allRanks) {
                String roleId = roleMappings.get(rankName.toLowerCase());
                if (roleId != null) {
                    Role role = guild.getRoleById(roleId);
                    if (role != null) {
                        rolesToAdd.add(role);
                    }
                }
            }
            
            // Get Discord roles to remove
            List<Role> rolesToRemove = new ArrayList<>();
            for (Role role : member.getRoles()) {
                String roleId = role.getId();
                if (roleMappings.containsValue(roleId)) {
                    boolean shouldKeep = false;
                    for (String rankName : allRanks) {
                        String mappedRoleId = roleMappings.get(rankName.toLowerCase());
                        if (roleId.equals(mappedRoleId)) {
                            shouldKeep = true;
                            break;
                        }
                    }
                    
                    if (!shouldKeep) {
                        rolesToRemove.add(role);
                    }
                }
            }
            
            // Apply role changes
            for (Role role : rolesToAdd) {
                if (!member.getRoles().contains(role)) {
                    guild.addRoleToMember(member, role).queue();
                }
            }
            
            for (Role role : rolesToRemove) {
                guild.removeRoleFromMember(member, role).queue();
            }
            
            return true;
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error syncing player roles with Discord", e);
            return false;
        }
    }
    
    /**
     * Sends a log message to the Discord log channel.
     *
     * @param auditLog The audit log
     */
    public void sendLogMessage(AuditLog auditLog) {
        // Check if Discord is enabled
        if (jda == null || logChannel == null) {
            return;
        }
        
        // Create embed
        net.dv8tion.jda.api.EmbedBuilder embed = new net.dv8tion.jda.api.EmbedBuilder();
        embed.setTitle("Audit Log");
        embed.setColor(getColorForActionType(auditLog.getActionType()));
        embed.setTimestamp(Instant.ofEpochMilli(auditLog.getTimestamp()));
        
        // Add fields
        embed.addField("Action", auditLog.getActionType().getDisplayName(), true);
        embed.addField("Actor", auditLog.getActorName(), true);
        embed.addField("Server", auditLog.getServer(), true);
        embed.addField("Details", auditLog.getDetails(), false);
        
        // Send message
        logChannel.sendMessageEmbeds(embed.build()).queue();
    }
    
    /**
     * Gets the color for an action type.
     *
     * @param actionType The action type
     * @return The color
     */
    private Color getColorForActionType(AuditLog.ActionType actionType) {
        switch (actionType) {
            case RANK_CREATE:
            case PERMISSION_ADD:
            case RANK_ADD:
                return Color.GREEN;
            case RANK_DELETE:
            case PERMISSION_REMOVE:
            case RANK_REMOVE:
                return Color.RED;
            case RANK_MODIFY:
            case RANK_SET:
            case PERMISSION_SET:
                return Color.ORANGE;
            case DISCORD_LINK:
            case DISCORD_UNLINK:
                return Color.BLUE;
            default:
                return Color.GRAY;
        }
    }
    
    /**
     * Gets the JDA instance.
     *
     * @return The JDA instance
     */
    public JDA getJda() {
        return jda;
    }
    
    /**
     * Gets the guild.
     *
     * @return The guild
     */
    public Guild getGuild() {
        return guild;
    }
    
    /**
     * Gets the log channel.
     *
     * @return The log channel
     */
    public TextChannel getLogChannel() {
        return logChannel;
    }
    
    /**
     * Gets the role mappings.
     *
     * @return The role mappings
     */
    public Map<String, String> getRoleMappings() {
        return roleMappings;
    }
} 