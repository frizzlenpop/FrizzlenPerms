package org.frizzlenpop.frizzlenPerms.commands;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.frizzlenpop.frizzlenPerms.FrizzlenPerms;
import org.frizzlenpop.frizzlenPerms.utils.MessageUtils;

import java.util.List;

/**
 * Command to open the GUI.
 */
public class GuiCommand implements SubCommand {
    
    private final FrizzlenPerms plugin;
    
    /**
     * Creates a new GuiCommand.
     *
     * @param plugin The plugin instance
     */
    public GuiCommand(FrizzlenPerms plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public String getName() {
        return "gui";
    }
    
    @Override
    public String getDescription() {
        return "Opens the GUI.";
    }
    
    @Override
    public String getUsage() {
        return "/frizzlenperms gui";
    }
    
    @Override
    public String getPermission() {
        return "frizzlenperms.gui";
    }
    
    @Override
    public int getMinArgs() {
        return 0;
    }
    
    @Override
    public List<String> getAliases() {
        return List.of("menu");
    }
    
    @Override
    public boolean isPlayerOnly() {
        return true;
    }
    
    @Override
    public boolean execute(CommandSender sender, String[] args) {
        Player player = (Player) sender;
        
        // Check if GUI is enabled
        if (!plugin.getConfigManager().isGuiEnabled()) {
            MessageUtils.sendMessage(player, "gui.disabled");
            return true;
        }
        
        // Open the GUI
        plugin.getGuiManager().openMainGui(player);
        return true;
    }
}