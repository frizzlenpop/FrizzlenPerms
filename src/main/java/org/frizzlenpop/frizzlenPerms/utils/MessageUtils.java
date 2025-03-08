package org.frizzlenpop.frizzlenPerms.utils;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.frizzlenpop.frizzlenPerms.FrizzlenPerms;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility class for handling plugin messages.
 */
public class MessageUtils {
    
    private static FrizzlenPerms plugin;
    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("%([^%]+)%");
    
    /**
     * Initializes the MessageUtils class.
     *
     * @param instance The plugin instance
     */
    public static void initialize(FrizzlenPerms instance) {
        plugin = instance;
    }
    
    /**
     * Sends a message to a CommandSender with placeholders.
     *
     * @param sender The receiver of the message
     * @param path The path to the message in messages.yml
     * @param placeholders Map of placeholders to replace
     */
    public static void sendMessage(CommandSender sender, String path, Map<String, String> placeholders) {
        String message = plugin.getConfigManager().getMessage(path, null);
        
        if (message == null || message.isEmpty()) {
            plugin.getLogger().warning("Message not found for path: " + path);
            return;
        }
        
        // Add prefix if not a header/footer
        if (!path.contains("header") && !path.contains("footer")) {
            String prefix = plugin.getConfigManager().getMessage("general.prefix", "&8[&6FrizzlenPerms&8] &r");
            if (prefix == null || prefix.isEmpty()) {
                plugin.getLogger().warning("Prefix not found in messages.yml");
            }
            message = prefix + message;
        }
        
        // Replace placeholders
        if (placeholders != null && !placeholders.isEmpty()) {
            try {
                Matcher matcher = PLACEHOLDER_PATTERN.matcher(message);
                StringBuffer buffer = new StringBuffer();
                
                while (matcher.find()) {
                    String placeholder = matcher.group(1);
                    String replacement = placeholders.getOrDefault(placeholder, matcher.group(0));
                    matcher.appendReplacement(buffer, Matcher.quoteReplacement(replacement));
                }
                
                matcher.appendTail(buffer);
                message = buffer.toString();
            } catch (Exception e) {
                plugin.getLogger().severe("Error replacing placeholders in message: " + path);
                e.printStackTrace();
            }
        }
        
        // Convert color codes
        try {
            message = ChatColor.translateAlternateColorCodes('&', message);
        } catch (Exception e) {
            plugin.getLogger().severe("Error translating color codes in message: " + path);
            e.printStackTrace();
        }
        
        // Send the message
        try {
            sender.sendMessage(message);
            plugin.getLogger().info("Sent message: " + message + " to " + sender.getName());
        } catch (Exception e) {
            plugin.getLogger().severe("Error sending message to " + sender.getName());
            e.printStackTrace();
        }
    }
    
    /**
     * Sends a message to a CommandSender without placeholders.
     *
     * @param sender The receiver of the message
     * @param path The path to the message in messages.yml
     */
    public static void sendMessage(CommandSender sender, String path) {
        sendMessage(sender, path, null);
    }
    
    /**
     * Formats a string with color codes.
     *
     * @param text The text to format
     * @return The formatted text
     */
    public static String formatColors(String text) {
        if (text == null) {
            return "";
        }
        return ChatColor.translateAlternateColorCodes('&', text);
    }
    
    /**
     * Strips color codes from a string.
     *
     * @param text The text to strip
     * @return The text without color codes
     */
    public static String stripColors(String text) {
        if (text == null) {
            return "";
        }
        return ChatColor.stripColor(ChatColor.translateAlternateColorCodes('&', text));
    }
    
    /**
     * Formats a message with color codes.
     *
     * @param message The message to format
     * @return The formatted message
     */
    public static String formatMessage(String message) {
        if (message == null) {
            return "";
        }
        return ChatColor.translateAlternateColorCodes('&', message);
    }
    
    /**
     * Formats a message with color codes and replaces placeholders.
     *
     * @param message The message to format
     * @param placeholders The placeholders to replace (key1, value1, key2, value2, ...)
     * @return The formatted message
     */
    public static String formatMessage(String message, String... placeholders) {
        if (message == null) {
            return "";
        }
        
        String formatted = message;
        
        // Replace placeholders
        for (int i = 0; i < placeholders.length - 1; i += 2) {
            String placeholder = placeholders[i];
            String value = placeholders[i + 1];
            
            if (placeholder != null && value != null) {
                formatted = formatted.replace(placeholder, value);
            }
        }
        
        // Translate color codes
        return ChatColor.translateAlternateColorCodes('&', formatted);
    }
    
    /**
     * Strips color codes from a message.
     *
     * @param message The message to strip
     * @return The stripped message
     */
    public static String stripColor(String message) {
        if (message == null) {
            return "";
        }
        return ChatColor.stripColor(message);
    }
    
    /**
     * Creates a centered message.
     *
     * @param message The message to center
     * @return The centered message
     */
    public static String centerMessage(String message) {
        if (message == null || message.isEmpty()) {
            return "";
        }
        
        int messagePxSize = 0;
        boolean previousCode = false;
        boolean isBold = false;
        
        for (char c : message.toCharArray()) {
            if (c == '§') {
                previousCode = true;
            } else if (previousCode) {
                previousCode = false;
                isBold = c == 'l' || c == 'L';
            } else {
                DefaultFontInfo dFI = DefaultFontInfo.getDefaultFontInfo(c);
                messagePxSize += isBold ? dFI.getBoldLength() : dFI.getLength();
                messagePxSize++;
            }
        }
        
        int halvedMessageSize = messagePxSize / 2;
        int toCompensate = 154 - halvedMessageSize;
        int spaceLength = DefaultFontInfo.SPACE.getLength() + 1;
        int compensated = 0;
        StringBuilder sb = new StringBuilder();
        
        while (compensated < toCompensate) {
            sb.append(" ");
            compensated += spaceLength;
        }
        
        return sb.toString() + message;
    }
    
    /**
     * Enum for default font info.
     */
    private enum DefaultFontInfo {
        A('A', 5),
        a('a', 5),
        B('B', 5),
        b('b', 5),
        C('C', 5),
        c('c', 5),
        D('D', 5),
        d('d', 5),
        E('E', 5),
        e('e', 5),
        F('F', 5),
        f('f', 4),
        G('G', 5),
        g('g', 5),
        H('H', 5),
        h('h', 5),
        I('I', 3),
        i('i', 1),
        J('J', 5),
        j('j', 5),
        K('K', 5),
        k('k', 4),
        L('L', 5),
        l('l', 1),
        M('M', 5),
        m('m', 5),
        N('N', 5),
        n('n', 5),
        O('O', 5),
        o('o', 5),
        P('P', 5),
        p('p', 5),
        Q('Q', 5),
        q('q', 5),
        R('R', 5),
        r('r', 5),
        S('S', 5),
        s('s', 5),
        T('T', 5),
        t('t', 4),
        U('U', 5),
        u('u', 5),
        V('V', 5),
        v('v', 5),
        W('W', 5),
        w('w', 5),
        X('X', 5),
        x('x', 5),
        Y('Y', 5),
        y('y', 5),
        Z('Z', 5),
        z('z', 5),
        NUM_1('1', 5),
        NUM_2('2', 5),
        NUM_3('3', 5),
        NUM_4('4', 5),
        NUM_5('5', 5),
        NUM_6('6', 5),
        NUM_7('7', 5),
        NUM_8('8', 5),
        NUM_9('9', 5),
        NUM_0('0', 5),
        EXCLAMATION_POINT('!', 1),
        AT_SYMBOL('@', 6),
        NUM_SIGN('#', 5),
        DOLLAR_SIGN('$', 5),
        PERCENT('%', 5),
        UP_ARROW('^', 5),
        AMPERSAND('&', 5),
        ASTERISK('*', 5),
        LEFT_PARENTHESIS('(', 4),
        RIGHT_PARENTHESIS(')', 4),
        MINUS('-', 5),
        UNDERSCORE('_', 5),
        PLUS_SIGN('+', 5),
        EQUALS_SIGN('=', 5),
        LEFT_CURLY_BRACE('{', 4),
        RIGHT_CURLY_BRACE('}', 4),
        LEFT_BRACKET('[', 3),
        RIGHT_BRACKET(']', 3),
        COLON(':', 1),
        SEMI_COLON(';', 1),
        DOUBLE_QUOTE('"', 3),
        SINGLE_QUOTE('\'', 1),
        LEFT_ARROW('<', 4),
        RIGHT_ARROW('>', 4),
        QUESTION_MARK('?', 5),
        SLASH('/', 5),
        BACK_SLASH('\\', 5),
        LINE('|', 1),
        TILDE('~', 5),
        TICK('`', 2),
        PERIOD('.', 1),
        COMMA(',', 1),
        SPACE(' ', 3),
        DEFAULT('a', 4);
        
        private final char character;
        private final int length;
        
        DefaultFontInfo(char character, int length) {
            this.character = character;
            this.length = length;
        }
        
        /**
         * Gets the character.
         *
         * @return The character
         */
        public char getCharacter() {
            return character;
        }
        
        /**
         * Gets the length.
         *
         * @return The length
         */
        public int getLength() {
            return length;
        }
        
        /**
         * Gets the bold length.
         *
         * @return The bold length
         */
        public int getBoldLength() {
            if (this == SPACE) {
                return getLength();
            }
            return length + 1;
        }
        
        /**
         * Gets the default font info for a character.
         *
         * @param c The character
         * @return The default font info
         */
        public static DefaultFontInfo getDefaultFontInfo(char c) {
            for (DefaultFontInfo dFI : DefaultFontInfo.values()) {
                if (dFI.getCharacter() == c) {
                    return dFI;
                }
            }
            return DEFAULT;
        }
    }
} 