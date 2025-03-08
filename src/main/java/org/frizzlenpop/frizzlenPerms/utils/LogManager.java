package org.frizzlenpop.frizzlenPerms.utils;

import org.frizzlenpop.frizzlenPerms.FrizzlenPerms;

import java.util.logging.Logger;

/**
 * Utility class for managing logging throughout the plugin.
 */
public class LogManager {
    
    private static Logger logger;
    
    /**
     * Sets the logger instance from the main plugin class.
     *
     * @param plugin The main plugin instance
     */
    public static void setLogger(FrizzlenPerms plugin) {
        logger = plugin.getLogger();
    }
    
    /**
     * Gets the logger instance.
     *
     * @return The logger instance
     */
    public static Logger getLogger() {
        if (logger == null) {
            throw new IllegalStateException("Logger has not been initialized. Call setLogger first.");
        }
        return logger;
    }
} 