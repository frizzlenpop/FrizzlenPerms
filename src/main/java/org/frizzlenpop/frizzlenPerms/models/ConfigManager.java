package org.frizzlenpop.frizzlenPerms.models;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.frizzlenpop.frizzlenPerms.FrizzlenPerms;
import org.frizzlenpop.frizzlenPerms.utils.LogManager;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;

/**
 * Manages plugin configuration settings and files.
 */
public class ConfigManager {
    
    private final FrizzlenPerms plugin;
    private FileConfiguration config;
    private File configFile;
    
    /**
     * Creates a new ConfigManager with the specified plugin instance.
     *
     * @param plugin The plugin instance
     */
    public ConfigManager(FrizzlenPerms plugin) {
        this.plugin = plugin;
        loadConfig();
    }
    
    /**
     * Loads the configuration file.
     */
    public void loadConfig() {
        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }
        
        configFile = new File(plugin.getDataFolder(), "config.yml");
        
        if (!configFile.exists()) {
            plugin.saveResource("config.yml", false);
        }
        
        config = YamlConfiguration.loadConfiguration(configFile);
        
        LogManager.getLogger().info("Configuration loaded successfully");
    }
    
    /**
     * Reloads the configuration file.
     */
    public void reloadConfig() {
        if (configFile == null) {
            configFile = new File(plugin.getDataFolder(), "config.yml");
        }
        
        config = YamlConfiguration.loadConfiguration(configFile);
        LogManager.getLogger().info("Configuration reloaded successfully");
    }
    
    /**
     * Saves the configuration file.
     */
    public void saveConfig() {
        if (config == null || configFile == null) {
            return;
        }
        
        try {
            config.save(configFile);
            LogManager.getLogger().info("Configuration saved successfully");
        } catch (IOException e) {
            LogManager.getLogger().log(Level.SEVERE, "Could not save config to " + configFile, e);
        }
    }
    
    /**
     * Gets the FileConfiguration instance.
     *
     * @return The FileConfiguration instance
     */
    public FileConfiguration getConfig() {
        if (config == null) {
            reloadConfig();
        }
        return config;
    }
    
    /**
     * Gets a string from the configuration.
     *
     * @param path The path to the string
     * @param defaultValue The default value to return if the path does not exist
     * @return The string at the specified path, or the default value if not found
     */
    public String getString(String path, String defaultValue) {
        return getConfig().getString(path, defaultValue);
    }
    
    /**
     * Gets an integer from the configuration.
     *
     * @param path The path to the integer
     * @param defaultValue The default value to return if the path does not exist
     * @return The integer at the specified path, or the default value if not found
     */
    public int getInt(String path, int defaultValue) {
        return getConfig().getInt(path, defaultValue);
    }
    
    /**
     * Gets a boolean from the configuration.
     *
     * @param path The path to the boolean
     * @param defaultValue The default value to return if the path does not exist
     * @return The boolean at the specified path, or the default value if not found
     */
    public boolean getBoolean(String path, boolean defaultValue) {
        return getConfig().getBoolean(path, defaultValue);
    }
    
    /**
     * Gets a string from the configuration.
     *
     * @param path The path to the string
     * @return The string at the specified path, or null if not found
     */
    public String getString(String path) {
        return getConfig().getString(path);
    }
    
    /**
     * Gets an integer from the configuration.
     *
     * @param path The path to the integer
     * @return The integer at the specified path, or 0 if not found
     */
    public int getInt(String path) {
        return getConfig().getInt(path);
    }
    
    /**
     * Gets a boolean from the configuration.
     *
     * @param path The path to the boolean
     * @return The boolean at the specified path, or false if not found
     */
    public boolean getBoolean(String path) {
        return getConfig().getBoolean(path);
    }
    
    /**
     * Gets the storage type from the configuration.
     *
     * @return The storage type (sqlite, mysql, or flatfile)
     */
    public String getStorageType() {
        return getString("storage.type", "sqlite");
    }
} 