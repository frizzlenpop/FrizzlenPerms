package org.frizzlenpop.frizzlenPerms.commands;

import org.bukkit.command.CommandSender;

import java.util.Collections;
import java.util.List;

/**
 * Interface for all plugin subcommands.
 */
public interface SubCommand {
    
    /**
     * Gets the name of the command.
     *
     * @return The name of the command
     */
    String getName();
    
    /**
     * Gets the description of the command.
     *
     * @return The description of the command
     */
    String getDescription();
    
    /**
     * Gets the usage message of the command.
     *
     * @return The usage message
     */
    String getUsage();
    
    /**
     * Gets the permission required to use the command.
     *
     * @return The permission
     */
    String getPermission();
    
    /**
     * Gets the minimum number of arguments required for the command.
     *
     * @return The minimum number of arguments
     */
    int getMinArgs();
    
    /**
     * Gets a list of aliases for the command.
     *
     * @return The list of aliases
     */
    default List<String> getAliases() {
        return Collections.emptyList();
    }
    
    /**
     * Checks if the command can only be used by players.
     *
     * @return True if the command can only be used by players
     */
    default boolean isPlayerOnly() {
        return false;
    }
    
    /**
     * Checks if the command can only be used by the console.
     *
     * @return True if the command can only be used by the console
     */
    default boolean isConsoleOnly() {
        return false;
    }
    
    /**
     * Checks if the sender has permission to use the command.
     *
     * @param sender The sender
     * @return True if the sender has permission
     */
    default boolean hasPermission(CommandSender sender) {
        return sender.hasPermission(getPermission()) || sender.isOp();
    }
    
    /**
     * Executes the command.
     *
     * @param sender The sender
     * @param args The command arguments
     * @return True if the command executed successfully, false to show usage
     */
    boolean execute(CommandSender sender, String[] args);
    
    /**
     * Provides tab completion options for the command.
     *
     * @param sender The sender
     * @param args The command arguments
     * @return A list of completion options
     */
    default List<String> tabComplete(CommandSender sender, String[] args) {
        return Collections.emptyList();
    }
} 