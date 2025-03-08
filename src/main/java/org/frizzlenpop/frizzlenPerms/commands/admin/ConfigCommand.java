package org.frizzlenpop.frizzlenPerms.commands.admin;

import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.frizzlenpop.frizzlenPerms.FrizzlenPerms;
import org.frizzlenpop.frizzlenPerms.commands.SubCommand;
import org.frizzlenpop.frizzlenPerms.utils.MessageUtils;

import java.util.*;

/**
 * Command to view and modify plugin configuration.
 */
public class ConfigCommand implements SubCommand {
    
    private final FrizzlenPerms plugin;
    
    /**
     * Creates a new ConfigCommand.
     *
     * @param plugin The plugin instance
     */
    public ConfigCommand(FrizzlenPerms plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public String getName() {
        return "config";
    }
    
    @Override
    public String getDescription() {
        return "Views or modifies plugin configuration.";
    }
    
    @Override
    public String getUsage() {
        return "/frizzlenperms config <get|set|reload> [path] [value]";
    }
    
    @Override
    public String getPermission() {
        return "frizzlenperms.admin.config";
    }
    
    @Override
    public int getMinArgs() {
        return 1;
    }
    
    @Override
    public List<String> getAliases() {
        return List.of("configuration", "settings");
    }
    
    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (args.length < 1) {
            MessageUtils.sendMessage(sender, "error.missing-arguments", Map.of(
                "usage", getUsage()
            ));
            return false;
        }
        
        String action = args[0].toLowerCase();
        
        switch (action) {
            case "get":
                return handleGet(sender, args);
            case "set":
                return handleSet(sender, args);
            case "reload":
                return handleReload(sender, args);
            default:
                MessageUtils.sendMessage(sender, "error.invalid-action", Map.of(
                    "action", action,
                    "valid", "get, set, reload"
                ));
                return false;
        }
    }
    
    /**
     * Handles the 'get' action.
     *
     * @param sender The command sender
     * @param args Command arguments
     * @return True if successful, false otherwise
     */
    private boolean handleGet(CommandSender sender, String[] args) {
        if (args.length < 2) {
            MessageUtils.sendMessage(sender, "error.missing-arguments", Map.of(
                "usage", "/frizzlenperms config get <path>"
            ));
            return false;
        }
        
        String path = args[1];
        FileConfiguration config = plugin.getConfig();
        
        if (!config.contains(path)) {
            MessageUtils.sendMessage(sender, "admin.config-path-not-found", Map.of(
                "path", path
            ));
            return false;
        }
        
        Object value = config.get(path);
        String valueStr = value != null ? value.toString() : "null";
        
        MessageUtils.sendMessage(sender, "admin.config-get", Map.of(
            "path", path,
            "value", valueStr,
            "type", value != null ? value.getClass().getSimpleName() : "null"
        ));
        
        return true;
    }
    
    /**
     * Handles the 'set' action.
     *
     * @param sender The command sender
     * @param args Command arguments
     * @return True if successful, false otherwise
     */
    private boolean handleSet(CommandSender sender, String[] args) {
        if (args.length < 3) {
            MessageUtils.sendMessage(sender, "error.missing-arguments", Map.of(
                "usage", "/frizzlenperms config set <path> <value>"
            ));
            return false;
        }
        
        String path = args[1];
        String valueStr = args[2];
        
        // Try to parse the value based on the existing type
        FileConfiguration config = plugin.getConfig();
        Object currentValue = config.get(path);
        Object newValue;
        
        if (currentValue == null) {
            // Path doesn't exist, try to guess the type
            if (valueStr.equalsIgnoreCase("true") || valueStr.equalsIgnoreCase("false")) {
                newValue = Boolean.parseBoolean(valueStr);
            } else {
                try {
                    newValue = Integer.parseInt(valueStr);
                } catch (NumberFormatException e1) {
                    try {
                        newValue = Double.parseDouble(valueStr);
                    } catch (NumberFormatException e2) {
                        newValue = valueStr;
                    }
                }
            }
        } else {
            // Try to convert to the existing type
            if (currentValue instanceof Boolean) {
                newValue = Boolean.parseBoolean(valueStr);
            } else if (currentValue instanceof Integer) {
                try {
                    newValue = Integer.parseInt(valueStr);
                } catch (NumberFormatException e) {
                    MessageUtils.sendMessage(sender, "error.invalid-number", Map.of(
                        "value", valueStr
                    ));
                    return false;
                }
            } else if (currentValue instanceof Double) {
                try {
                    newValue = Double.parseDouble(valueStr);
                } catch (NumberFormatException e) {
                    MessageUtils.sendMessage(sender, "error.invalid-number", Map.of(
                        "value", valueStr
                    ));
                    return false;
                }
            } else if (currentValue instanceof List) {
                // For lists, we'll just add the value
                @SuppressWarnings("unchecked")
                List<String> list = (List<String>) currentValue;
                list.add(valueStr);
                newValue = list;
            } else {
                // Default to string
                newValue = valueStr;
            }
        }
        
        // Set the value
        config.set(path, newValue);
        plugin.saveConfig();
        
        // Reload the config manager
        plugin.getConfigManager().reloadConfig();
        
        MessageUtils.sendMessage(sender, "admin.config-set", Map.of(
            "path", path,
            "value", newValue.toString(),
            "type", newValue.getClass().getSimpleName()
        ));
        
        return true;
    }
    
    /**
     * Handles the 'reload' action.
     *
     * @param sender The command sender
     * @param args Command arguments
     * @return True if successful, false otherwise
     */
    private boolean handleReload(CommandSender sender, String[] args) {
        plugin.reloadConfig();
        plugin.getConfigManager().reloadConfig();
        
        MessageUtils.sendMessage(sender, "admin.config-reloaded");
        
        return true;
    }
    
    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            // Suggest actions
            List<String> actions = Arrays.asList("get", "set", "reload");
            String partial = args[0].toLowerCase();
            
            return actions.stream()
                .filter(action -> action.startsWith(partial))
                .collect(java.util.stream.Collectors.toList());
        } else if (args.length == 2 && (args[0].equalsIgnoreCase("get") || args[0].equalsIgnoreCase("set"))) {
            // Suggest config paths
            String partial = args[1].toLowerCase();
            List<String> paths = getConfigPaths(plugin.getConfig(), "");
            
            return paths.stream()
                .filter(path -> path.toLowerCase().startsWith(partial))
                .collect(java.util.stream.Collectors.toList());
        } else if (args.length == 3 && args[0].equalsIgnoreCase("set")) {
            // Suggest values based on the path
            String path = args[1];
            Object currentValue = plugin.getConfig().get(path);
            
            if (currentValue instanceof Boolean) {
                return Arrays.asList("true", "false");
            } else if (currentValue instanceof List) {
                // For lists, suggest adding a new item
                return List.of("<new-item>");
            }
        }
        
        return Collections.emptyList();
    }
    
    /**
     * Gets all configuration paths recursively.
     *
     * @param config The configuration
     * @param parent The parent path
     * @return A list of all paths
     */
    private List<String> getConfigPaths(FileConfiguration config, String parent) {
        List<String> paths = new ArrayList<>();
        
        for (String key : config.getKeys(false)) {
            String path = parent.isEmpty() ? key : parent + "." + key;
            paths.add(path);
            
            if (config.isConfigurationSection(path)) {
                paths.addAll(getConfigPaths(config, path));
            }
        }
        
        return paths;
    }
} 