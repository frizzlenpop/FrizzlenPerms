package org.frizzlenpop.frizzlenPerms;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.logging.Level;

/**
 * Manages plugin configuration files.
 */
public class ConfigManager {
    
    private final FrizzlenPerms plugin;
    
    // Configuration files
    private File configFile;
    private FileConfiguration config;
    
    private File messagesFile;
    private FileConfiguration messages;
    
    /**
     * Creates a new ConfigManager.
     *
     * @param plugin The plugin instance
     */
    public ConfigManager(FrizzlenPerms plugin) {
        this.plugin = plugin;
        
        // Load configuration files
        loadConfig();
        loadMessages();
    }
    
    /**
     * Loads the main configuration file.
     */
    private void loadConfig() {
        // Create config file if it doesn't exist
        configFile = new File(plugin.getDataFolder(), "config.yml");
        if (!configFile.exists()) {
            plugin.saveResource("config.yml", false);
        }
        
        // Load config
        config = YamlConfiguration.loadConfiguration(configFile);
        
        // Check for updates to config
        InputStream defaultConfigStream = plugin.getResource("config.yml");
        if (defaultConfigStream != null) {
            YamlConfiguration defaultConfig = YamlConfiguration.loadConfiguration(
                new InputStreamReader(defaultConfigStream, StandardCharsets.UTF_8));
            
            // Add missing options
            for (String key : defaultConfig.getKeys(true)) {
                if (!config.contains(key)) {
                    config.set(key, defaultConfig.get(key));
                    plugin.getLogger().info("Added missing config option: " + key);
                }
            }
            
            // Save if changes were made
            try {
                config.save(configFile);
            } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE, "Could not save config.yml", e);
            }
        }
    }
    
    /**
     * Loads the messages file.
     */
    private void loadMessages() {
        // Create messages file if it doesn't exist
        messagesFile = new File(plugin.getDataFolder(), "messages.yml");
        if (!messagesFile.exists()) {
            plugin.saveResource("messages.yml", false);
        }
        
        // Load messages
        messages = YamlConfiguration.loadConfiguration(messagesFile);
        
        // Check for updates to messages
        InputStream defaultMessagesStream = plugin.getResource("messages.yml");
        if (defaultMessagesStream != null) {
            YamlConfiguration defaultMessages = YamlConfiguration.loadConfiguration(
                new InputStreamReader(defaultMessagesStream, StandardCharsets.UTF_8));
            
            // Add missing options
            for (String key : defaultMessages.getKeys(true)) {
                if (!messages.contains(key)) {
                    messages.set(key, defaultMessages.get(key));
                    plugin.getLogger().info("Added missing message: " + key);
                }
            }
            
            // Save if changes were made
            try {
                messages.save(messagesFile);
            } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE, "Could not save messages.yml", e);
            }
        }
    }
    
    /**
     * Reloads all configuration files.
     */
    public void reloadConfig() {
        loadConfig();
        loadMessages();
    }
    
    /**
     * Gets a message from the messages file.
     *
     * @param path The path to the message
     * @param defaultValue The default value if the message is not found
     * @return The message
     */
    public String getMessage(String path, String defaultValue) {
        return messages.getString(path, defaultValue);
    }
    
    /**
     * Gets a list of messages from the messages file.
     *
     * @param path The path to the messages
     * @return The list of messages
     */
    public List<String> getMessageList(String path) {
        return messages.getStringList(path);
    }
    
    /**
     * Gets the storage type from the config.
     *
     * @return The storage type (mysql, sqlite, flatfile)
     */
    public String getStorageType() {
        return config.getString("storage.type", "sqlite").toLowerCase();
    }
    
    /**
     * Gets the MySQL host from the config.
     *
     * @return The MySQL host
     */
    public String getMySQLHost() {
        return config.getString("storage.mysql.host", "localhost");
    }
    
    /**
     * Gets the MySQL port from the config.
     *
     * @return The MySQL port
     */
    public int getMySQLPort() {
        return config.getInt("storage.mysql.port", 3306);
    }
    
    /**
     * Gets the MySQL database from the config.
     *
     * @return The MySQL database
     */
    public String getMySQLDatabase() {
        return config.getString("storage.mysql.database", "frizzlenperms");
    }
    
    /**
     * Gets the MySQL username from the config.
     *
     * @return The MySQL username
     */
    public String getMySQLUsername() {
        return config.getString("storage.mysql.username", "root");
    }
    
    /**
     * Gets the MySQL password from the config.
     *
     * @return The MySQL password
     */
    public String getMySQLPassword() {
        return config.getString("storage.mysql.password", "");
    }
    
    /**
     * Gets the MySQL connection pool size from the config.
     *
     * @return The MySQL connection pool size
     */
    public int getMySQLPoolSize() {
        return config.getInt("storage.mysql.pool-size", 10);
    }
    
    /**
     * Gets the SQLite database file path from the config.
     *
     * @return The SQLite database file path
     */
    public String getSQLiteFile() {
        return config.getString("storage.sqlite.file", "database.db");
    }
    
    /**
     * Gets the SQLite connection pool size from the config.
     *
     * @return The SQLite connection pool size
     */
    public int getSQLitePoolSize() {
        return config.getInt("storage.sqlite.pool-size", 5);
    }
    
    /**
     * Gets the FlatFile directory from the config.
     *
     * @return The FlatFile directory
     */
    public String getFlatFileDirectory() {
        return config.getString("storage.flatfile.directory", "data");
    }
    
    /**
     * Checks if Discord integration is enabled.
     *
     * @return True if Discord integration is enabled
     */
    public boolean isDiscordEnabled() {
        return config.getBoolean("discord.enabled", false);
    }
    
    /**
     * Gets the Discord bot token.
     *
     * @return The Discord bot token
     */
    public String getDiscordToken() {
        return config.getString("discord.token", "");
    }
    
    /**
     * Gets the Discord guild ID.
     *
     * @return The Discord guild ID
     */
    public String getDiscordGuildId() {
        return config.getString("discord.guild_id", "");
    }
    
    /**
     * Gets the Discord log channel ID.
     *
     * @return The Discord log channel ID
     */
    public String getDiscordLogChannelId() {
        return config.getString("discord.log_channel_id", "");
    }
    
    /**
     * Gets the Discord role mappings.
     *
     * @return The Discord role mappings
     */
    public ConfigurationSection getDiscordRoleMappings() {
        return config.getConfigurationSection("discord.role_mappings");
    }
    
    /**
     * Gets the Discord sync interval in minutes.
     *
     * @return The Discord sync interval
     */
    public int getDiscordSyncInterval() {
        return config.getInt("discord.sync_interval", 30);
    }
    
    /**
     * Checks if sync is enabled.
     *
     * @return True if sync is enabled
     */
    public boolean isSyncEnabled() {
        return config.getBoolean("sync.enabled", false);
    }
    
    /**
     * Gets the sync server address from the config.
     *
     * @return The sync server address
     */
    public String getSyncServerAddress() {
        return config.getString("sync.server-address", "localhost");
    }
    
    /**
     * Gets the sync server port from the config.
     *
     * @return The sync server port
     */
    public int getSyncServerPort() {
        return config.getInt("sync.server-port", 8080);
    }
    
    /**
     * Gets the sync API key from the config.
     *
     * @return The sync API key
     */
    public String getSyncApiKey() {
        return config.getString("sync.api-key", "");
    }
    
    /**
     * Checks if the GUI is enabled.
     *
     * @return True if the GUI is enabled
     */
    public boolean isGuiEnabled() {
        return config.getBoolean("gui.enabled", true);
    }
    
    /**
     * Gets the number of commands to display per help page.
     *
     * @return The number of commands per page
     */
    public int getCommandsPerPage() {
        return config.getInt("help.commands-per-page", 8);
    }
    
    /**
     * Gets the maximum number of ranks a player can have.
     *
     * @return The maximum number of ranks per player
     */
    public int getMaxRanksPerPlayer() {
        return config.getInt("ranks.max-ranks-per-player", 5);
    }
    
    /**
     * Gets the maximum number of days to keep audit logs.
     *
     * @return The maximum number of days to keep audit logs
     */
    public int getAuditLogRetentionDays() {
        return config.getInt("audit.retention-days", 30);
    }
    
    /**
     * Checks if audit logging is enabled.
     *
     * @return True if audit logging is enabled
     */
    public boolean isAuditLoggingEnabled() {
        return config.getBoolean("audit.enabled", true);
    }
    
    /**
     * Checks if file logging is enabled for audit logs.
     *
     * @return True if file logging is enabled
     */
    public boolean isAuditFileLoggingEnabled() {
        return config.getBoolean("audit.file-logging", true);
    }
    
    /**
     * Gets the audit log file path.
     *
     * @return The audit log file path
     */
    public String getAuditLogFile() {
        return config.getString("audit.log-file", "audit.log");
    }
    
    /**
     * Gets the maximum number of audit logs to return per query.
     *
     * @return The maximum number of audit logs
     */
    public int getMaxAuditLogs() {
        return config.getInt("audit.max-logs", 100);
    }
    
    /**
     * Gets the default rank name.
     *
     * @return The default rank name
     */
    public String getDefaultRankName() {
        return config.getString("ranks.default", "default");
    }
    
    /**
     * Gets the admin rank name.
     *
     * @return The admin rank name
     */
    public String getAdminRankName() {
        return config.getString("ranks.admin", "admin");
    }
    
    /**
     * Gets the default rank permissions.
     *
     * @return The default rank permissions
     */
    public List<String> getDefaultRankPermissions() {
        return config.getStringList("ranks.default-permissions");
    }
    
    /**
     * Gets the admin rank permissions.
     *
     * @return The admin rank permissions
     */
    public List<String> getAdminRankPermissions() {
        return config.getStringList("ranks.admin-permissions");
    }
    
    /**
     * Gets the server name from the config.
     *
     * @return The server name
     */
    public String getServerName() {
        return config.getString("server-name", "default");
    }
    
    /**
     * Checks if anti-abuse features are enabled.
     *
     * @return True if anti-abuse features are enabled
     */
    public boolean isAntiAbuseEnabled() {
        return config.getBoolean("security.anti-abuse", true);
    }
    
    /**
     * Checks if logging is enabled.
     *
     * @return True if logging is enabled
     */
    public boolean isLoggingEnabled() {
        return config.getBoolean("logging.enabled", true);
    }
    
    /**
     * Checks if logging should be saved to a file.
     *
     * @return True if logging should be saved to a file
     */
    public boolean isLoggingSavedToFile() {
        return config.getBoolean("logging.save-to-file", true);
    }
    
    /**
     * Gets the maximum number of log entries to keep.
     *
     * @return The maximum number of log entries
     */
    public int getMaxLogEntries() {
        return config.getInt("logging.max-entries", 1000);
    }
    
    /**
     * Gets a string value from the config.
     *
     * @param path The path to the value
     * @param def The default value
     * @return The string value
     */
    public String getString(String path, String def) {
        return config.getString(path, def);
    }

    /**
     * Gets a boolean value from the config.
     *
     * @param path The path to the value
     * @param defaultValue The default value if not found
     * @return The boolean value
     */
    public boolean getBoolean(String path, boolean defaultValue) {
        return config.getBoolean(path, defaultValue);
    }

    /**
     * Gets an integer value from the config.
     *
     * @param path The path to the value
     * @param defaultValue The default value if not found
     * @return The integer value
     */
    public int getInt(String path, int defaultValue) {
        return config.getInt(path, defaultValue);
    }

    /**
     * Gets a configuration section from the config.
     *
     * @param path The path to the section
     * @return The configuration section, or null if not found
     */
    public ConfigurationSection getConfigurationSection(String path) {
        return config.getConfigurationSection(path);
    }

    /**
     * Saves the configuration to disk.
     */
    public void saveConfig() {
        try {
            config.save(configFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not save config.yml", e);
        }
    }

    /**
     * Saves the messages to disk.
     */
    public void saveMessages() {
        try {
            messages.save(messagesFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not save messages.yml", e);
        }
    }

    /**
     * Sets a value in the config.
     *
     * @param path The path to set
     * @param value The value to set
     */
    public void set(String path, Object value) {
        if (path != null && !path.isEmpty()) {
            config.set(path, value);
            saveConfig();
        }
    }
} 