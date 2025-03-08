package org.frizzlenpop.frizzlenPerms.gui;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.frizzlenpop.frizzlenPerms.FrizzlenPerms;
import org.frizzlenpop.frizzlenPerms.models.PlayerData;
import org.frizzlenpop.frizzlenPerms.utils.MessageUtils;

import java.text.SimpleDateFormat;
import java.util.*;

/**
 * GUI for managing players.
 */
public class PlayerManagementGui implements PermissionGui {
    
    private final FrizzlenPerms plugin;
    private final Player player;
    private final int page;
    private final Inventory inventory;
    private final SimpleDateFormat dateFormat;
    
    private static final int PLAYERS_PER_PAGE = 36;
    
    /**
     * Creates a new PlayerManagementGui.
     *
     * @param plugin The plugin instance
     * @param player The player viewing the GUI
     * @param page The page number
     */
    public PlayerManagementGui(FrizzlenPerms plugin, Player player, int page) {
        this.plugin = plugin;
        this.player = player;
        this.page = Math.max(1, page);
        this.dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        
        // Create inventory
        String title = MessageUtils.formatColors("&8[&6FrizzlenPerms&8] &fPlayers (Page " + this.page + ")");
        this.inventory = Bukkit.createInventory(null, 54, title);
        
        // Initialize items
        initializeItems();
    }
    
    /**
     * Initializes the items in the GUI.
     */
    private void initializeItems() {
        // Get all player data
        List<PlayerData> allPlayerData = plugin.getDataManager().getAllPlayerData();
        
        // Sort by last login time (most recent first)
        allPlayerData.sort((p1, p2) -> Long.compare(p2.getLastLogin(), p1.getLastLogin()));
        
        // Calculate total pages
        int totalPages = (int) Math.ceil((double) allPlayerData.size() / PLAYERS_PER_PAGE);
        
        // Calculate start and end indices
        int startIndex = (page - 1) * PLAYERS_PER_PAGE;
        int endIndex = Math.min(startIndex + PLAYERS_PER_PAGE, allPlayerData.size());
        
        // Add player heads
        for (int i = startIndex; i < endIndex; i++) {
            PlayerData playerData = allPlayerData.get(i);
            UUID playerUuid = playerData.getUuid();
            String playerName = playerData.getName();
            
            ItemStack playerHead = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta meta = (SkullMeta) playerHead.getItemMeta();
            
            // Try to set owner
            OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(playerUuid);
            meta.setOwningPlayer(offlinePlayer);
            
            // Set display name
            meta.setDisplayName(MessageUtils.formatColors("&6" + playerName));
            
            // Set lore
            List<String> lore = new ArrayList<>();
            lore.add(MessageUtils.formatColors("&7UUID: &f" + playerUuid));
            
            // Add last login time
            if (playerData.getLastLogin() > 0) {
                lore.add(MessageUtils.formatColors("&7Last Login: &f" + dateFormat.format(new Date(playerData.getLastLogin()))));
            } else {
                lore.add(MessageUtils.formatColors("&7Last Login: &fNever"));
            }
            
            // Add last seen time
            if (playerData.getLastSeen() > 0) {
                lore.add(MessageUtils.formatColors("&7Last Seen: &f" + dateFormat.format(new Date(playerData.getLastSeen()))));
            } else {
                lore.add(MessageUtils.formatColors("&7Last Seen: &fNever"));
            }
            
            // Add ranks
            Set<String> ranks = playerData.getRanks();
            if (!ranks.isEmpty()) {
                lore.add(MessageUtils.formatColors("&7Ranks: &f" + String.join(", ", ranks)));
            } else {
                lore.add(MessageUtils.formatColors("&7Ranks: &fNone"));
            }
            
            // Add click instructions
            lore.add("");
            lore.add(MessageUtils.formatColors("&eLeft-click to view player info"));
            lore.add(MessageUtils.formatColors("&eRight-click to manage permissions"));
            lore.add(MessageUtils.formatColors("&eShift-click to manage ranks"));
            
            meta.setLore(lore);
            playerHead.setItemMeta(meta);
            
            // Add to inventory
            inventory.setItem(i - startIndex, playerHead);
        }
        
        // Add navigation buttons
        if (page > 1) {
            // Previous page button
            ItemStack prevButton = new ItemStack(Material.ARROW);
            ItemMeta prevMeta = prevButton.getItemMeta();
            prevMeta.setDisplayName(MessageUtils.formatColors("&6Previous Page"));
            prevButton.setItemMeta(prevMeta);
            inventory.setItem(45, prevButton);
        }
        
        if (page < totalPages) {
            // Next page button
            ItemStack nextButton = new ItemStack(Material.ARROW);
            ItemMeta nextMeta = nextButton.getItemMeta();
            nextMeta.setDisplayName(MessageUtils.formatColors("&6Next Page"));
            nextButton.setItemMeta(nextMeta);
            inventory.setItem(53, nextButton);
        }
        
        // Back button
        ItemStack backButton = new ItemStack(Material.BARRIER);
        ItemMeta backMeta = backButton.getItemMeta();
        backMeta.setDisplayName(MessageUtils.formatColors("&cBack to Main Menu"));
        backButton.setItemMeta(backMeta);
        inventory.setItem(49, backButton);
        
        // Fill empty slots with glass panes
        ItemStack filler = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta fillerMeta = filler.getItemMeta();
        fillerMeta.setDisplayName(ChatColor.RESET.toString());
        filler.setItemMeta(fillerMeta);
        
        for (int i = 0; i < inventory.getSize(); i++) {
            if (inventory.getItem(i) == null) {
                inventory.setItem(i, filler);
            }
        }
    }
    
    @Override
    public Inventory getInventory() {
        return inventory;
    }
    
    @Override
    public void handleClick(int slot, ClickType clickType) {
        // Check if the slot is a player slot
        if (slot >= 0 && slot < PLAYERS_PER_PAGE) {
            // Calculate the index in the player list
            int playerIndex = (page - 1) * PLAYERS_PER_PAGE + slot;
            List<PlayerData> allPlayerData = plugin.getDataManager().getAllPlayerData();
            
            // Sort by last login time (most recent first)
            allPlayerData.sort((p1, p2) -> Long.compare(p2.getLastLogin(), p1.getLastLogin()));
            
            // Check if the index is valid
            if (playerIndex < allPlayerData.size()) {
                PlayerData playerData = allPlayerData.get(playerIndex);
                UUID playerUuid = playerData.getUuid();
                
                // Handle different click types
                if (clickType == ClickType.LEFT) {
                    // View player info
                    plugin.getGuiManager().openPlayerInfoGui(player, playerUuid);
                } else if (clickType == ClickType.RIGHT) {
                    // Manage permissions
                    plugin.getGuiManager().openPlayerPermissionsGui(player, playerUuid, 1);
                } else if (clickType == ClickType.SHIFT_LEFT || clickType == ClickType.SHIFT_RIGHT) {
                    // Manage ranks
                    plugin.getGuiManager().openPlayerRanksGui(player, playerUuid, 1);
                }
            }
        } else if (slot == 45 && page > 1) {
            // Previous page
            plugin.getGuiManager().openPlayerManagementGui(player, page - 1);
        } else if (slot == 53) {
            // Next page
            List<PlayerData> allPlayerData = plugin.getDataManager().getAllPlayerData();
            int totalPages = (int) Math.ceil((double) allPlayerData.size() / PLAYERS_PER_PAGE);
            
            if (page < totalPages) {
                plugin.getGuiManager().openPlayerManagementGui(player, page + 1);
            }
        } else if (slot == 49) {
            // Back to main menu
            plugin.getGuiManager().openMainGui(player);
        }
    }
    
    @Override
    public void update() {
        initializeItems();
    }
} 