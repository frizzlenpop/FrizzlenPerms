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
        String authors = String.join(", ", plugin.getDescription().getAuthors());
        String website = plugin.getDescription().getWebsite();
        
        // Send plugin info
        MessageUtils.sendMessage(sender, "admin.version-header");
        MessageUtils.sendMessage(sender, "admin.version-info", Map.of(
            "version", version,
            "authors", authors,
            "website", website != null ? website : "N/A"
        ));
        
        // Show build info if available
        if (plugin.getDescription().getCommands().containsKey("build")) {
            Map<String, Object> buildCommand = (Map<String, Object>) plugin.getDescription().getCommands().get("build");
            String buildNumber = buildCommand.containsKey("description") ? buildCommand.get("description").toString() : "N/A";
            String buildDate = buildCommand.containsKey("usage") ? buildCommand.get("usage").toString() : "N/A";
            
            MessageUtils.sendMessage(sender, "admin.version-build", Map.of(
                "build", buildNumber,
                "date", buildDate
            ));
        }
        
        return true;
    }
} 