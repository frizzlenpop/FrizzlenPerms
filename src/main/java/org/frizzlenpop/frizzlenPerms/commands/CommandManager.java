package org.frizzlenpop.frizzlenPerms.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.frizzlenpop.frizzlenPerms.FrizzlenPerms;
import org.frizzlenpop.frizzlenPerms.commands.admin.*;
import org.frizzlenpop.frizzlenPerms.commands.player.*;
import org.frizzlenpop.frizzlenPerms.commands.rank.*;
import org.frizzlenpop.frizzlenPerms.utils.MessageUtils;

import java.util.*;

/**
 * Manages all plugin commands and their implementations.
 */
public class CommandManager implements CommandExecutor, TabCompleter {
    
    private final FrizzlenPerms plugin;
    private final Map<String, SubCommand> commands;
    private final Map<String, SubCommand> aliases;
    
    /**
     * Creates a new CommandManager.
     *
     * @param plugin The plugin instance
     */
    public CommandManager(FrizzlenPerms plugin) {
        this.plugin = plugin;
        this.commands = new HashMap<>();
        this.aliases = new HashMap<>();
    }
    
    /**
     * Registers all plugin commands.
     */
    public void registerCommands() {
        // Register the main command
        plugin.getCommand("frizzlenperms").setExecutor(this);
        plugin.getCommand("frizzlenperms").setTabCompleter(this);
        
        // Register subcommands
        // Help command
        registerCommand(new HelpCommand(plugin));
        
        // Admin commands
        registerCommand(new ReloadCommand(plugin));
        registerCommand(new VersionCommand(plugin));
        registerCommand(new ConfigCommand(plugin));
        registerCommand(new org.frizzlenpop.frizzlenPerms.commands.admin.SyncCommand(plugin));
        registerCommand(new AuditLogCommand(plugin));
        registerCommand(new org.frizzlenpop.frizzlenPerms.commands.admin.InfoCommand(plugin));
        registerCommand(new ImportCommand(plugin));
        registerCommand(new ExportCommand(plugin));
        registerCommand(new PurgeCommand(plugin));
        registerCommand(new CloneCommand(plugin));
        
        // Player commands
        registerCommand(new org.frizzlenpop.frizzlenPerms.commands.player.InfoCommand(plugin));
        registerCommand(new ListCommand(plugin));
        registerCommand(new RanksCommand(plugin));
        registerCommand(new PermissionsCommand(plugin));
        registerCommand(new SetRankCommand(plugin));
        registerCommand(new AddRankCommand(plugin));
        registerCommand(new RemoveRankCommand(plugin));
        registerCommand(new CheckPermissionCommand(plugin));
        registerCommand(new AddPermissionCommand(plugin));
        registerCommand(new RemovePermissionCommand(plugin));
        registerCommand(new AddTempRankCommand(plugin));
        registerCommand(new RemoveTempRankCommand(plugin));
        registerCommand(new AddTempPermissionCommand(plugin));
        registerCommand(new RemoveTempPermissionCommand(plugin));
        
        // Rank commands
        registerCommand(new RankCreateCommand(plugin));
        registerCommand(new RankInfoCommand(plugin));
        registerCommand(new RankDeleteCommand(plugin));
        registerCommand(new RankListCommand(plugin));
        registerCommand(new RankSetDefaultCommand(plugin));
        registerCommand(new RankAddPermissionCommand(plugin));
        registerCommand(new RankRemovePermissionCommand(plugin));
        registerCommand(new RankSetPrefixCommand(plugin));
        registerCommand(new RankSetSuffixCommand(plugin));
        registerCommand(new RankSetWeightCommand(plugin));
        registerCommand(new RankSetDisplayNameCommand(plugin));
        registerCommand(new RankSetColorCommand(plugin));
        registerCommand(new RankAddInheritanceCommand(plugin));
        registerCommand(new RankRemoveInheritanceCommand(plugin));
        
        // GUI command
        registerCommand(new GuiCommand(plugin));
        
        // Discord command
        registerCommand(new DiscordCommand(plugin));
        
        // Audit commands
        registerCommand(new AuditLogCommand(plugin));
        
        // Sync commands
        registerCommand(new org.frizzlenpop.frizzlenPerms.commands.player.SyncCommand(plugin));
        registerCommand(new SyncPushCommand(plugin));
        registerCommand(new SyncPullCommand(plugin));
        
        plugin.getLogger().info("Registered " + commands.size() + " commands with " + aliases.size() + " aliases.");
    }
    
    /**
     * Registers a subcommand.
     *
     * @param command The subcommand to register
     */
    private void registerCommand(SubCommand command) {
        commands.put(command.getName().toLowerCase(), command);
        
        // Register aliases
        for (String alias : command.getAliases()) {
            aliases.put(alias.toLowerCase(), command);
        }
    }
    
    /**
     * Gets a command by name or alias.
     *
     * @param name The command name or alias
     * @return The command, or null if not found
     */
    public SubCommand getCommand(String name) {
        String lowercaseName = name.toLowerCase();
        return commands.getOrDefault(lowercaseName, aliases.get(lowercaseName));
    }
    
    /**
     * Gets all registered commands.
     *
     * @return A collection of all commands
     */
    public Collection<SubCommand> getCommands() {
        return commands.values();
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            // Show help message
            showHelpMessage(sender);
            return true;
        }
        
        String subCommandName = args[0].toLowerCase();
        SubCommand subCommand = commands.getOrDefault(subCommandName, aliases.get(subCommandName));
        
        if (subCommand == null) {
            // Unknown command
            MessageUtils.sendMessage(sender, "general.unknown-command", Map.of("command", label));
            return true;
        }
        
        // Check permission
        if (!subCommand.hasPermission(sender)) {
            MessageUtils.sendMessage(sender, "general.no-permission");
            return true;
        }
        
        // Check if command is player-only
        if (subCommand.isPlayerOnly() && !(sender instanceof Player)) {
            MessageUtils.sendMessage(sender, "general.player-only");
            return true;
        }
        
        // Check if command is console-only
        if (subCommand.isConsoleOnly() && sender instanceof Player) {
            MessageUtils.sendMessage(sender, "general.console-only");
            return true;
        }
        
        // Strip the first argument (subcommand name)
        String[] subArgs = Arrays.copyOfRange(args, 1, args.length);
        
        // Check argument count
        if (subArgs.length < subCommand.getMinArgs()) {
            MessageUtils.sendMessage(sender, "general.invalid-arguments", Map.of("command", label));
            sender.sendMessage(subCommand.getUsage());
            return true;
        }
        
        // Execute the command
        try {
            if (!subCommand.execute(sender, subArgs)) {
                // Command returned usage
                sender.sendMessage(subCommand.getUsage());
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Error executing command " + subCommandName + ": " + e.getMessage());
            e.printStackTrace();
            MessageUtils.sendMessage(sender, "general.internal-error");
        }
        
        return true;
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            // Completing the subcommand
            List<String> completions = new ArrayList<>();
            String partialCommand = args[0].toLowerCase();
            
            // Add all commands the player has permission for
            for (SubCommand subCommand : commands.values()) {
                if (subCommand.hasPermission(sender) && subCommand.getName().toLowerCase().startsWith(partialCommand)) {
                    completions.add(subCommand.getName());
                }
            }
            
            // Add aliases
            for (Map.Entry<String, SubCommand> entry : aliases.entrySet()) {
                String aliasName = entry.getKey();
                SubCommand subCommand = entry.getValue();
                
                if (subCommand.hasPermission(sender) && aliasName.startsWith(partialCommand)) {
                    completions.add(aliasName);
                }
            }
            
            return completions;
        } else if (args.length > 1) {
            // Completing arguments for a subcommand
            String subCommandName = args[0].toLowerCase();
            SubCommand subCommand = commands.getOrDefault(subCommandName, aliases.get(subCommandName));
            
            if (subCommand != null && subCommand.hasPermission(sender)) {
                String[] subArgs = Arrays.copyOfRange(args, 1, args.length);
                return subCommand.tabComplete(sender, subArgs);
            }
        }
        
        return Collections.emptyList();
    }
    
    /**
     * Shows the help message to a command sender.
     *
     * @param sender The command sender
     */
    private void showHelpMessage(CommandSender sender) {
        MessageUtils.sendMessage(sender, "help.header", Map.of("page", "1", "total", "1"));
        
        for (SubCommand subCommand : commands.values()) {
            if (subCommand.hasPermission(sender)) {
                MessageUtils.sendMessage(sender, "help.command", Map.of(
                    "command", subCommand.getName(), 
                    "description", subCommand.getDescription()
                ));
            }
        }
        
        MessageUtils.sendMessage(sender, "help.footer");
    }
} 