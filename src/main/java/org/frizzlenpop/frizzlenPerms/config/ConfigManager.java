package org.frizzlenpop.frizzlenPerms.config;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.frizzlenpop.frizzlenPerms.FrizzlenPerms;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

/**
 * Manages configuration files for the plugin.
 */
public class ConfigManager {
    
    private final FrizzlenPerms plugin;
    
    // Configuration files
    private FileConfiguration mainConfig;
    private FileConfiguration ranksConfig;
    private FileConfiguration discordConfig;
    private FileConfiguration messagesConfig;
    
    // File objects
    private File mainConfigFile;
    private File ranksConfigFile;
    private File discordConfigFile;
    private File messagesConfigFile;
    
    // Default values
    private final Map<String, Object> defaultMainConfig = new HashMap<>();
    private final Map<String, Object> defaultRanksConfig = new HashMap<>();
    private final Map<String, Object> defaultDiscordConfig = new HashMap<>();
    private final Map<String, Object> defaultMessagesConfig = new HashMap<>();
    
    /**
     * Creates a new ConfigManager instance.
     *
     * @param plugin The plugin instance
     */
    public ConfigManager(FrizzlenPerms plugin) {
        this.plugin = plugin;
        setupDefaultValues();
    }
    
    /**
     * Sets up default configuration values.
     */
    private void setupDefaultValues() {
        // Main config defaults
        defaultMainConfig.put("storage.type", "sqlite"); // sqlite, mysql, or flatfile
        defaultMainConfig.put("storage.mysql.host", "localhost");
        defaultMainConfig.put("storage.mysql.port", 3306);
        defaultMainConfig.put("storage.mysql.database", "frizzlenperms");
        defaultMainConfig.put("storage.mysql.username", "root");
        defaultMainConfig.put("storage.mysql.password", "password");
        defaultMainConfig.put("storage.mysql.useSSL", false);
        defaultMainConfig.put("storage.mysql.poolSize", 10);
        
        defaultMainConfig.put("sync.enabled", false);
        defaultMainConfig.put("sync.redis.host", "localhost");
        defaultMainConfig.put("sync.redis.port", 6379);
        defaultMainConfig.put("sync.redis.password", "");
        defaultMainConfig.put("sync.redis.database", 0);
        
        defaultMainConfig.put("logging.enabled", true);
        defaultMainConfig.put("logging.log_rank_changes", true);
        defaultMainConfig.put("logging.log_permission_changes", true);
        defaultMainConfig.put("logging.log_suspicious_activity", true);
        
        defaultMainConfig.put("anti_abuse.enabled", true);
        defaultMainConfig.put("anti_abuse.auto_revoke_illegal_permissions", true);
        defaultMainConfig.put("anti_abuse.alert_admins_on_exploit_attempts", true);
        defaultMainConfig.put("anti_abuse.auto_ban_exploiters", false);
        
        defaultMainConfig.put("chat.format", "{rank_prefix}{player_name}{rank_suffix}: {message}");
        defaultMainConfig.put("chat.use_rank_colors", true);
        
        defaultMainConfig.put("auto_rankup.enabled", false);
        defaultMainConfig.put("auto_rankup.check_interval_minutes", 30);
        
        defaultMainConfig.put("rank_decay.enabled", false);
        defaultMainConfig.put("rank_decay.check_interval_hours", 24);
        defaultMainConfig.put("rank_decay.inactivity_days", 30);
        
        // Ranks config defaults
        defaultRanksConfig.put("ranks.default.prefix", "[Player] ");
        defaultRanksConfig.put("ranks.default.suffix", "");
        defaultRanksConfig.put("ranks.default.chat_color", "&7");
        defaultRanksConfig.put("ranks.default.is_default", true);
        defaultRanksConfig.put("ranks.default.weight", 0);
        defaultRanksConfig.put("ranks.default.permissions", new String[]{"essentials.help", "essentials.spawn"});
        
        defaultRanksConfig.put("ranks.moderator.prefix", "[Mod] ");
        defaultRanksConfig.put("ranks.moderator.suffix", "");
        defaultRanksConfig.put("ranks.moderator.chat_color", "&a");
        defaultRanksConfig.put("ranks.moderator.inherit_from", "default");
        defaultRanksConfig.put("ranks.moderator.weight", 50);
        defaultRanksConfig.put("ranks.moderator.permissions", new String[]{"essentials.kick", "essentials.mute"});
        
        defaultRanksConfig.put("ranks.admin.prefix", "[Admin] ");
        defaultRanksConfig.put("ranks.admin.suffix", "");
        defaultRanksConfig.put("ranks.admin.chat_color", "&c");
        defaultRanksConfig.put("ranks.admin.inherit_from", "moderator");
        defaultRanksConfig.put("ranks.admin.weight", 100);
        defaultRanksConfig.put("ranks.admin.permissions", new String[]{"*"});
        
        // Discord config defaults
        defaultDiscordConfig.put("enabled", false);
        defaultDiscordConfig.put("bot_token", "YOUR_DISCORD_BOT_TOKEN");
        defaultDiscordConfig.put("sync.enabled", true);
        defaultDiscordConfig.put("sync.guild_id", "123456789012345678");
        defaultDiscordConfig.put("sync.roles.default", "987654321098765432");
        defaultDiscordConfig.put("sync.roles.moderator", "876543210987654321");
        defaultDiscordConfig.put("sync.roles.admin", "765432109876543210");
        defaultDiscordConfig.put("sync.sync_on_join", true);
        defaultDiscordConfig.put("sync.discord_to_minecraft", true);
        
        // Messages config defaults
        defaultMessagesConfig.put("prefix", "&8[&bFrizzlenPerms&8] &7");
        defaultMessagesConfig.put("no_permission", "&cYou don't have permission to use this command.");
        defaultMessagesConfig.put("player_not_found", "&cPlayer not found.");
        defaultMessagesConfig.put("rank_not_found", "&cRank not found.");
        defaultMessagesConfig.put("rank_created", "&aRank {rank} has been created.");
        defaultMessagesConfig.put("rank_deleted", "&aRank {rank} has been deleted.");
        defaultMessagesConfig.put("rank_set", "&aSet {player}'s rank to {rank}.");
        defaultMessagesConfig.put("rank_set_temporary", "&aSet {player}'s rank to {rank} for {duration}.");
        defaultMessagesConfig.put("rank_expired", "&cYour {rank} rank has expired.");
        defaultMessagesConfig.put("rank_expiring_soon", "&eYour {rank} rank expires in {time}.");
        defaultMessagesConfig.put("permission_added", "&aAdded permission {permission} to {target}.");
        defaultMessagesConfig.put("permission_removed", "&aRemoved permission {permission} from {target}.");
        defaultMessagesConfig.put("permission_added_temporary", "&aAdded permission {permission} to {target} for {duration}.");
        defaultMessagesConfig.put("permission_expired", "&cYour permission {permission} has expired.");
        defaultMessagesConfig.put("discord_linked", "&aYour Discord account has been linked successfully.");
        defaultMessagesConfig.put("discord_unlinked", "&aYour Discord account has been unlinked.");
    }
    
    /**
     * Loads all configuration files.
     */
    public void loadConfigs() {
        loadMainConfig();
        loadRanksConfig();
        loadDiscordConfig();
        loadMessagesConfig();
    }
    
    /**
     * Loads the main configuration file.
     */
    private void loadMainConfig() {
        mainConfigFile = new File(plugin.getDataFolder(), "config.yml");
        
        if (!mainConfigFile.exists()) {
            plugin.saveResource("config.yml", false);
        }
        
        mainConfig = YamlConfiguration.loadConfiguration(mainConfigFile);
        
        // Set default values if they don't exist
        boolean needsSave = false;
        for (Map.Entry<String, Object> entry : defaultMainConfig.entrySet()) {
            if (!mainConfig.contains(entry.getKey())) {
                mainConfig.set(entry.getKey(), entry.getValue());
                needsSave = true;
            }
        }
        
        if (needsSave) {
            saveMainConfig();
        }
    }
    
    /**
     * Loads the ranks configuration file.
     */
    private void loadRanksConfig() {
        ranksConfigFile = new File(plugin.getDataFolder(), "ranks.yml");
        
        if (!ranksConfigFile.exists()) {
            try {
                plugin.getDataFolder().mkdirs();
                ranksConfigFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE, "Could not create ranks.yml", e);
            }
        }
        
        ranksConfig = YamlConfiguration.loadConfiguration(ranksConfigFile);
        
        // Set default values if they don't exist
        boolean needsSave = false;
        for (Map.Entry<String, Object> entry : defaultRanksConfig.entrySet()) {
            if (!ranksConfig.contains(entry.getKey())) {
                ranksConfig.set(entry.getKey(), entry.getValue());
                needsSave = true;
            }
        }
        
        if (needsSave) {
            saveRanksConfig();
        }
    }
    
    /**
     * Loads the Discord configuration file.
     */
    private void loadDiscordConfig() {
        discordConfigFile = new File(plugin.getDataFolder(), "discord.yml");
        
        if (!discordConfigFile.exists()) {
            try {
                plugin.getDataFolder().mkdirs();
                discordConfigFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE, "Could not create discord.yml", e);
            }
        }
        
        discordConfig = YamlConfiguration.loadConfiguration(discordConfigFile);
        
        // Set default values if they don't exist
        boolean needsSave = false;
        for (Map.Entry<String, Object> entry : defaultDiscordConfig.entrySet()) {
            if (!discordConfig.contains(entry.getKey())) {
                discordConfig.set(entry.getKey(), entry.getValue());
                needsSave = true;
            }
        }
        
        if (needsSave) {
            saveDiscordConfig();
        }
    }
    
    /**
     * Loads the messages configuration file.
     */
    private void loadMessagesConfig() {
        messagesConfigFile = new File(plugin.getDataFolder(), "messages.yml");
        
        if (!messagesConfigFile.exists()) {
            try {
                plugin.getDataFolder().mkdirs();
                messagesConfigFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE, "Could not create messages.yml", e);
            }
        }
        
        messagesConfig = YamlConfiguration.loadConfiguration(messagesConfigFile);
        
        // Set default values if they don't exist
        boolean needsSave = false;
        for (Map.Entry<String, Object> entry : defaultMessagesConfig.entrySet()) {
            if (!messagesConfig.contains(entry.getKey())) {
                messagesConfig.set(entry.getKey(), entry.getValue());
                needsSave = true;
            }
        }
        
        if (needsSave) {
            saveMessagesConfig();
        }
    }
    
    /**
     * Saves the main configuration file.
     */
    public void saveMainConfig() {
        try {
            mainConfig.save(mainConfigFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not save config.yml", e);
        }
    }
    
    /**
     * Saves the ranks configuration file.
     */
    public void saveRanksConfig() {
        try {
            ranksConfig.save(ranksConfigFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not save ranks.yml", e);
        }
    }
    
    /**
     * Saves the Discord configuration file.
     */
    public void saveDiscordConfig() {
        try {
            discordConfig.save(discordConfigFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not save discord.yml", e);
        }
    }
    
    /**
     * Saves the messages configuration file.
     */
    public void saveMessagesConfig() {
        try {
            messagesConfig.save(messagesConfigFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not save messages.yml", e);
        }
    }
    
    /**
     * Gets the main configuration.
     *
     * @return The main configuration
     */
    public FileConfiguration getMainConfig() {
        return mainConfig;
    }
    
    /**
     * Gets the ranks configuration.
     *
     * @return The ranks configuration
     */
    public FileConfiguration getRanksConfig() {
        return ranksConfig;
    }
    
    /**
     * Gets the Discord configuration.
     *
     * @return The Discord configuration
     */
    public FileConfiguration getDiscordConfig() {
        return discordConfig;
    }
    
    /**
     * Gets the messages configuration.
     *
     * @return The messages configuration
     */
    public FileConfiguration getMessagesConfig() {
        return messagesConfig;
    }
    
    /**
     * Checks if Discord integration is enabled.
     *
     * @return True if Discord integration is enabled
     */
    public boolean isDiscordEnabled() {
        return discordConfig.getBoolean("enabled", false);
    }
    
    /**
     * Gets the storage type from the configuration.
     *
     * @return The storage type (sqlite, mysql, or flatfile)
     */
    public String getStorageType() {
        return mainConfig.getString("storage.type", "sqlite");
    }
    
    /**
     * Checks if server synchronization is enabled.
     *
     * @return True if server synchronization is enabled
     */
    public boolean isSyncEnabled() {
        return mainConfig.getBoolean("sync.enabled", false);
    }
    
    /**
     * Gets a message from the messages configuration.
     *
     * @param key The message key
     * @return The message
     */
    public String getMessage(String key) {
        return messagesConfig.getString(key, "Message not found: " + key);
    }
    
    /**
     * Gets a message from the messages configuration with placeholders replaced.
     *
     * @param key The message key
     * @param placeholders The placeholders to replace (key1, value1, key2, value2, ...)
     * @return The message with placeholders replaced
     */
    public String getMessage(String key, String... placeholders) {
        String message = getMessage(key);
        
        if (placeholders != null && placeholders.length > 0) {
            for (int i = 0; i < placeholders.length; i += 2) {
                if (i + 1 < placeholders.length) {
                    message = message.replace("{" + placeholders[i] + "}", placeholders[i + 1]);
                }
            }
        }
        
        return message;
    }
} 