package org.frizzlenpop.frizzlenPerms.commands.admin;

import org.bukkit.command.CommandSender;
import org.frizzlenpop.frizzlenPerms.FrizzlenPerms;
import org.frizzlenpop.frizzlenPerms.commands.SubCommand;
import org.frizzlenpop.frizzlenPerms.utils.MessageUtils;

import java.util.List;
import java.util.Map;

/**
 * Command to display plugin version information.
 */
public class VersionCommand implements SubCommand {
    
    private final FrizzlenPerms plugin;
    
    /**
     * Creates a new VersionCommand.
     *
     * @param plugin The plugin instance
     */
    public VersionCommand(FrizzlenPerms plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public String getName() {
        return "version";
    }
    
    @Override
    public String getDescription() {
        return "Shows the plugin version information.";
    }
    
    @Override
    public String getUsage() {
        return "/frizzlenperms version";
    }
    
    @Override
    public String getPermission() {
        return "frizzlenperms.admin.version";
    }
    
    @Override
    public int getMinArgs() {
        return 0;
    }
    
    @Override
    public List<String> getAliases() {
        return List.of("ver", "v", "about");
    }
    
    @Override
    public boolean execute(CommandSender sender, String[] args) {
        // Get plugin version info
        String version = plugin.getDescription().getVersion();
        List<String> authorsList = plugin.getDescription().getAuthors();
        String authors = authorsList != null && !authorsList.isEmpty() ? String.join(", ", authorsList) : "Unknown";
        String website = plugin.getDescription().getWebsite();
        
        // Send plugin info
        MessageUtils.sendMessage(sender, "admin.version-header");
        MessageUtils.sendMessage(sender, "admin.version-info", Map.of(
            "version", version != null ? version : "Unknown",
            "authors", authors,
            "website", website != null ? website : "N/A"
        ));
        
        // Show build info if available
        Map<String, Map<String, Object>> commands = plugin.getDescription().getCommands();
        if (commands != null && commands.containsKey("build")) {
            try {
                Map<String, Object> buildCommand = commands.get("build");
                if (buildCommand != null) {
                    String buildNumber = buildCommand.getOrDefault("description", "N/A").toString();
                    String buildDate = buildCommand.getOrDefault("usage", "N/A").toString();
                    
                    MessageUtils.sendMessage(sender, "admin.version-build", Map.of(
                        "build", buildNumber,
                        "date", buildDate
                    ));
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to read build information: " + e.getMessage());
            }
        }
        
        return true;
    }
} 