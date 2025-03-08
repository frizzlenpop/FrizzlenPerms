package org.frizzlenpop.frizzlenPerms.gui;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.frizzlenpop.frizzlenPerms.FrizzlenPerms;
import org.frizzlenpop.frizzlenPerms.utils.MessageUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Main GUI for the plugin.
 */
public class MainGui implements PermissionGui {
    
    private final FrizzlenPerms plugin;
    private final Player player;
    private final Inventory inventory;
    
    /**
     * Creates a new MainGui.
     *
     * @param plugin The plugin instance
     * @param player The player viewing the GUI
     */
    public MainGui(FrizzlenPerms plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
        
        // Create inventory
        String title = MessageUtils.formatColors("&8[&6FrizzlenPerms&8] &fMain Menu");
        this.inventory = Bukkit.createInventory(null, 27, title);
        
        // Initialize items
        initializeItems();
    }
    
    /**
     * Initializes the items in the GUI.
     */
    private void initializeItems() {
        // Player management
        ItemStack playerItem = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta playerMeta = (SkullMeta) playerItem.getItemMeta();
        playerMeta.setOwningPlayer(player);
        playerMeta.setDisplayName(MessageUtils.formatColors("&6Player Management"));
        List<String> playerLore = new ArrayList<>();
        playerLore.add(MessageUtils.formatColors("&7Click to manage players"));
        playerLore.add(MessageUtils.formatColors("&7Add/remove ranks and permissions"));
        playerMeta.setLore(playerLore);
        playerItem.setItemMeta(playerMeta);
        inventory.setItem(11, playerItem);
        
        // Rank management
        ItemStack rankItem = new ItemStack(Material.GOLDEN_HELMET);
        ItemMeta rankMeta = rankItem.getItemMeta();
        rankMeta.setDisplayName(MessageUtils.formatColors("&6Rank Management"));
        List<String> rankLore = new ArrayList<>();
        rankLore.add(MessageUtils.formatColors("&7Click to manage ranks"));
        rankLore.add(MessageUtils.formatColors("&7Create/delete ranks and modify permissions"));
        rankMeta.setLore(rankLore);
        rankItem.setItemMeta(rankMeta);
        inventory.setItem(13, rankItem);
        
        // Settings
        ItemStack settingsItem = new ItemStack(Material.REDSTONE_TORCH);
        ItemMeta settingsMeta = settingsItem.getItemMeta();
        settingsMeta.setDisplayName(MessageUtils.formatColors("&6Settings"));
        List<String> settingsLore = new ArrayList<>();
        settingsLore.add(MessageUtils.formatColors("&7Click to manage plugin settings"));
        settingsMeta.setLore(settingsLore);
        settingsItem.setItemMeta(settingsMeta);
        inventory.setItem(15, settingsItem);
        
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
        switch (slot) {
            case 11:
                // Player management
                plugin.getGuiManager().openPlayerManagementGui(player, 1);
                break;
            case 13:
                // Rank management
                plugin.getGuiManager().openRankManagementGui(player, 1);
                break;
            case 15:
                // Settings
                player.closeInventory();
                player.sendMessage(MessageUtils.formatColors("&cSettings GUI not implemented yet."));
                break;
        }
    }
    
    @Override
    public void update() {
        // Nothing to update in the main GUI
    }
} 