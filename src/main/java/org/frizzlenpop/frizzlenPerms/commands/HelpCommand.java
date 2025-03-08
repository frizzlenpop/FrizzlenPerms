package org.frizzlenpop.frizzlenPerms.commands;

import org.bukkit.command.CommandSender;
import org.frizzlenpop.frizzlenPerms.FrizzlenPerms;
import org.frizzlenpop.frizzlenPerms.utils.MessageUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Command to display help information.
 */
public class HelpCommand implements SubCommand {
    
    private final FrizzlenPerms plugin;
    
    /**
     * Creates a new HelpCommand.
     *
     * @param plugin The plugin instance
     */
    public HelpCommand(FrizzlenPerms plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public String getName() {
        return "help";
    }
    
    @Override
    public String getDescription() {
        return "Displays help information.";
    }
    
    @Override
    public String getUsage() {
        return "/frizzlenperms help [page|command]";
    }
    
    @Override
    public String getPermission() {
        return "frizzlenperms.help";
    }
    
    @Override
    public int getMinArgs() {
        return 0;
    }
    
    @Override
    public List<String> getAliases() {
        List<String> aliases = new ArrayList<>();
        aliases.add("?");
        return aliases;
    }
    
    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (args.length == 0) {
            // Show first page
            showHelpPage(sender, 1);
            return true;
        }
        
        // Check if the argument is a page number
        try {
            int page = Integer.parseInt(args[0]);
            showHelpPage(sender, page);
            return true;
        } catch (NumberFormatException e) {
            // Not a number, check if it's a command
            String commandName = args[0].toLowerCase();
            SubCommand command = plugin.getCommandManager().getCommand(commandName);
            
            if (command != null && command.hasPermission(sender)) {
                // Show command help
                showCommandHelp(sender, command);
                return true;
            } else {
                // Unknown command or no permission
                MessageUtils.sendMessage(sender, "help.unknown-command", Map.of("command", commandName));
                return false;
            }
        }
    }
    
    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            List<String> completions = new ArrayList<>();
            
            // Add page numbers
            int totalPages = getTotalPages(sender);
            for (int i = 1; i <= totalPages; i++) {
                completions.add(String.valueOf(i));
            }
            
            // Add command names
            completions.addAll(plugin.getCommandManager().getCommands().stream()
                .filter(cmd -> cmd.hasPermission(sender))
                .map(SubCommand::getName)
                .collect(Collectors.toList()));
            
            return completions.stream()
                .filter(s -> s.toLowerCase().startsWith(args[0].toLowerCase()))
                .collect(Collectors.toList());
        }
        
        return List.of();
    }
    
    /**
     * Shows a help page to a command sender.
     *
     * @param sender The command sender
     * @param page The page number
     */
    private void showHelpPage(CommandSender sender, int page) {
        List<SubCommand> availableCommands = plugin.getCommandManager().getCommands().stream()
            .filter(cmd -> cmd.hasPermission(sender))
            .collect(Collectors.toList());
        
        int totalPages = getTotalPages(sender);
        
        // Validate page number
        if (page < 1 || page > totalPages) {
            MessageUtils.sendMessage(sender, "help.invalid-page", Map.of(
                "page", String.valueOf(page),
                "total", String.valueOf(totalPages)
            ));
            return;
        }
        
        // Calculate commands for this page
        int commandsPerPage = plugin.getConfigManager().getCommandsPerPage();
        int startIndex = (page - 1) * commandsPerPage;
        int endIndex = Math.min(startIndex + commandsPerPage, availableCommands.size());
        
        // Show header
        MessageUtils.sendMessage(sender, "help.header", Map.of(
            "page", String.valueOf(page),
            "total", String.valueOf(totalPages)
        ));
        
        // Show commands
        for (int i = startIndex; i < endIndex; i++) {
            SubCommand command = availableCommands.get(i);
            MessageUtils.sendMessage(sender, "help.command", Map.of(
                "command", "frizzlenperms " + command.getName(),
                "description", command.getDescription()
            ));
        }
        
        // Show footer
        MessageUtils.sendMessage(sender, "help.footer");
    }
    
    /**
     * Shows detailed help for a specific command.
     *
     * @param sender The command sender
     * @param command The command
     */
    private void showCommandHelp(CommandSender sender, SubCommand command) {
        MessageUtils.sendMessage(sender, "help.command-header", Map.of("command", command.getName()));
        
        // Description
        MessageUtils.sendMessage(sender, "help.command-description", Map.of(
            "description", command.getDescription()
        ));
        
        // Usage
        MessageUtils.sendMessage(sender, "help.command-usage", Map.of(
            "usage", command.getUsage()
        ));
        
        // Aliases
        List<String> aliases = command.getAliases();
        if (!aliases.isEmpty()) {
            MessageUtils.sendMessage(sender, "help.command-aliases", Map.of(
                "aliases", String.join(", ", aliases)
            ));
        }
        
        // Permission
        MessageUtils.sendMessage(sender, "help.command-permission", Map.of(
            "permission", command.getPermission()
        ));
        
        MessageUtils.sendMessage(sender, "help.command-footer");
    }
    
    /**
     * Gets the total number of help pages for a command sender.
     *
     * @param sender The command sender
     * @return The total number of pages
     */
    private int getTotalPages(CommandSender sender) {
        int availableCommands = (int) plugin.getCommandManager().getCommands().stream()
            .filter(cmd -> cmd.hasPermission(sender))
            .count();
        
        int commandsPerPage = plugin.getConfigManager().getCommandsPerPage();
        return (int) Math.ceil((double) availableCommands / commandsPerPage);
    }

    private void showHelpMessage(CommandSender sender) {
        MessageUtils.sendMessage(sender, "help.header", Map.of("page", "1", "total", "1"));
        
        for (SubCommand subCommand : plugin.getCommandManager().getCommands()) {
            if (subCommand.hasPermission(sender)) {
                MessageUtils.sendMessage(sender, "help.command", Map.of(
                    "command", "frizzlenperms " + subCommand.getName(),
                    "description", subCommand.getDescription()
                ));
            }
        }
        
        MessageUtils.sendMessage(sender, "help.footer");
    }
} 