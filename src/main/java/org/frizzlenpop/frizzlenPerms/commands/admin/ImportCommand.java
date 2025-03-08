package org.frizzlenpop.frizzlenPerms.commands.admin;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.frizzlenpop.frizzlenPerms.FrizzlenPerms;
import org.frizzlenpop.frizzlenPerms.commands.SubCommand;
import org.frizzlenpop.frizzlenPerms.utils.MessageUtils;

import java.io.File;
import java.util.List;
import java.util.Map;

/**
 * Command to import data from other permission plugins.
 */
public class ImportCommand implements SubCommand {
    
    private final FrizzlenPerms plugin;
    
    /**
     * Creates a new ImportCommand.
     *
     * @param plugin The plugin instance
     */
    public ImportCommand(FrizzlenPerms plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public String getName() {
        return "import";
    }
    
    @Override
    public String getDescription() {
        return "Imports data from other permission plugins.";
    }
    
    @Override
    public String getUsage() {
        return "/frizzlenperms import <luckperms|permissionsex|groupmanager|file> [path]";
    }
    
    @Override
    public String getPermission() {
        return "frizzlenperms.admin.import";
    }
    
    @Override
    public int getMinArgs() {
        return 1;
    }
    
    @Override
    public List<String> getAliases() {
        return List.of("load");
    }
    
    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (args.length < 1) {
            MessageUtils.sendMessage(sender, "admin.import-usage");
            return false;
        }
        
        String source = args[0].toLowerCase();
        String path = args.length > 1 ? args[1] : null;
        
        // Check if source is valid
        if (!List.of("luckperms", "permissionsex", "groupmanager", "file").contains(source)) {
            MessageUtils.sendMessage(sender, "admin.import-invalid-source");
            return false;
        }
        
        // Validate file path if provided
        if ("file".equals(source) && (path == null || path.isEmpty())) {
            MessageUtils.sendMessage(sender, "admin.import-missing-path");
            return false;
        }
        
        // If it's a file, check if it exists
        if ("file".equals(source) && path != null) {
            File importFile = new File(path);
            if (!importFile.exists() || !importFile.isFile()) {
                MessageUtils.sendMessage(sender, "admin.import-file-not-found", Map.of("path", path));
                return false;
            }
        }
        
        // Send starting import message
        MessageUtils.sendMessage(sender, "admin.import-starting", Map.of("source", source));
        
        // Run the import async
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            long startTime = System.currentTimeMillis();
            boolean success = false;
            
            try {
                // Import from different sources
                switch (source) {
                    case "luckperms":
                        success = importFromLuckPerms(sender);
                        break;
                    case "permissionsex":
                        success = importFromPermissionsEx(sender);
                        break;
                    case "groupmanager":
                        success = importFromGroupManager(sender);
                        break;
                    case "file":
                        success = importFromFile(sender, path);
                        break;
                }
                
                long endTime = System.currentTimeMillis();
                long duration = endTime - startTime;
                
                // Send completion message
                if (success) {
                    plugin.getServer().getScheduler().runTask(plugin, () -> {
                        MessageUtils.sendMessage(sender, "admin.import-success", Map.of(
                            "source", source,
                            "time", String.valueOf(duration)
                        ));
                    });
                } else {
                    plugin.getServer().getScheduler().runTask(plugin, () -> {
                        MessageUtils.sendMessage(sender, "admin.import-failed", Map.of("source", source));
                    });
                }
            } catch (Exception e) {
                plugin.getLogger().severe("Error during import from " + source + ": " + e.getMessage());
                e.printStackTrace();
                
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    MessageUtils.sendMessage(sender, "admin.import-error", Map.of(
                        "source", source,
                        "error", e.getMessage()
                    ));
                });
            }
        });
        
        return true;
    }
    
    private boolean importFromLuckPerms(CommandSender sender) {
        // Implementation would go here
        // This is just a placeholder for now
        plugin.getLogger().info("Import from LuckPerms not yet implemented");
        return false;
    }
    
    private boolean importFromPermissionsEx(CommandSender sender) {
        // Implementation would go here
        // This is just a placeholder for now
        plugin.getLogger().info("Import from PermissionsEx not yet implemented");
        return false;
    }
    
    private boolean importFromGroupManager(CommandSender sender) {
        // Implementation would go here
        // This is just a placeholder for now
        plugin.getLogger().info("Import from GroupManager not yet implemented");
        return false;
    }
    
    private boolean importFromFile(CommandSender sender, String path) {
        // Implementation would go here
        // This is just a placeholder for now
        plugin.getLogger().info("Import from file not yet implemented");
        return false;
    }
    
    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return List.of("luckperms", "permissionsex", "groupmanager", "file");
        } else if (args.length == 2 && "file".equals(args[0].toLowerCase())) {
            if (sender instanceof Player) {
                return List.of("plugins/FrizzlenPerms/export.yml");
            }
        }
        return List.of();
    }
} 