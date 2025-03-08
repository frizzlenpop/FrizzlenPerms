package org.frizzlenpop.frizzlenPerms.gui;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.frizzlenpop.frizzlenPerms.FrizzlenPerms;
import org.frizzlenpop.frizzlenPerms.models.Rank;
import org.frizzlenpop.frizzlenPerms.utils.MessageUtils;

import java.util.*;

/**
 * GUI for managing rank inheritance.
 */
public class RankInheritanceGui implements PermissionGui {
    
    private static final int ITEMS_PER_PAGE = 45;
    
    private final FrizzlenPerms plugin;
    private final Player viewer;
    private final String rankName;
    private final int page;
    private final Inventory inventory;
    
    /**
     * Creates a new RankInheritanceGui.
     *
     * @param plugin The plugin instance
     * @param viewer The player viewing the GUI
     * @param rankName The name of the rank
     * @param page The page number
     */
    public RankInheritanceGui(FrizzlenPerms plugin, Player viewer, String rankName, int page) {
        this.plugin = plugin;
        this.viewer = viewer;
        this.rankName = rankName;
        this.page = Math.max(1, page);
        this.inventory = Bukkit.createInventory(null, 54, "Rank Inheritance");
        
        initializeItems();
    }
    
    /**
     * Initializes the items in the GUI.
     */
    private void initializeItems() {
        Rank rank = plugin.getRankManager().getRank(rankName);
        if (rank == null) {
            return;
        }
        
        // Get all inherited ranks
        List<String> inheritedRanks = new ArrayList<>(rank.getInheritance());
        Collections.sort(inheritedRanks);
        
        // Calculate total pages
        int totalPages = (inheritedRanks.size() + ITEMS_PER_PAGE - 1) / ITEMS_PER_PAGE;
        if (totalPages == 0) totalPages = 1;
        
        // Add rank items
        int startIndex = (page - 1) * ITEMS_PER_PAGE;
        int endIndex = Math.min(startIndex + ITEMS_PER_PAGE, inheritedRanks.size());
        
        for (int i = startIndex; i < endIndex; i++) {
            String inheritedRankName = inheritedRanks.get(i);
            Rank inheritedRank = plugin.getRankManager().getRank(inheritedRankName);
            if (inheritedRank != null) {
                ItemStack item = new ItemStack(Material.GOLD_INGOT);
                ItemMeta meta = item.getItemMeta();
                meta.setDisplayName(MessageUtils.formatColors("&e" + inheritedRank.getName()));
                
                List<String> lore = new ArrayList<>();
                lore.add(MessageUtils.formatColors("&7Weight: &f" + inheritedRank.getWeight()));
                if (inheritedRank.getPrefix() != null) {
                    lore.add(MessageUtils.formatColors("&7Prefix: &f" + inheritedRank.getPrefix()));
                }
                if (inheritedRank.getSuffix() != null) {
                    lore.add(MessageUtils.formatColors("&7Suffix: &f" + inheritedRank.getSuffix()));
                }
                lore.add(MessageUtils.formatColors("&7Click to remove"));
                
                meta.setLore(lore);
                item.setItemMeta(meta);
                inventory.setItem(i - startIndex, item);
            }
        }
        
        // Add inheritance button (if has permission)
        if (viewer.hasPermission("frizzlenperms.admin.rankinheritance")) {
            ItemStack addItem = new ItemStack(Material.EMERALD);
            ItemMeta addMeta = addItem.getItemMeta();
            addMeta.setDisplayName(MessageUtils.formatColors("&aAdd Inheritance"));
            List<String> addLore = new ArrayList<>();
            addLore.add(MessageUtils.formatColors("&7Click to add inheritance"));
            addMeta.setLore(addLore);
            addItem.setItemMeta(addMeta);
            inventory.setItem(47, addItem);
        }
        
        // Navigation items
        if (page > 1) {
            ItemStack prevItem = new ItemStack(Material.ARROW);
            ItemMeta prevMeta = prevItem.getItemMeta();
            prevMeta.setDisplayName(MessageUtils.formatColors("&ePrevious Page"));
            prevItem.setItemMeta(prevMeta);
            inventory.setItem(45, prevItem);
        }
        
        if (page < totalPages) {
            ItemStack nextItem = new ItemStack(Material.ARROW);
            ItemMeta nextMeta = nextItem.getItemMeta();
            nextMeta.setDisplayName(MessageUtils.formatColors("&eNext Page"));
            nextItem.setItemMeta(nextMeta);
            inventory.setItem(53, nextItem);
        }
        
        // Back button
        ItemStack backItem = new ItemStack(Material.BARRIER);
        ItemMeta backMeta = backItem.getItemMeta();
        backMeta.setDisplayName(MessageUtils.formatColors("&eBack"));
        backItem.setItemMeta(backMeta);
        inventory.setItem(49, backItem);
        
        // Page indicator
        ItemStack pageItem = new ItemStack(Material.PAPER);
        ItemMeta pageMeta = pageItem.getItemMeta();
        pageMeta.setDisplayName(MessageUtils.formatColors("&ePage " + page + "/" + totalPages));
        pageItem.setItemMeta(pageMeta);
        inventory.setItem(51, pageItem);
    }
    
    @Override
    public Inventory getInventory() {
        return inventory;
    }
    
    @Override
    public void handleClick(int slot, ClickType clickType) {
        Rank rank = plugin.getRankManager().getRank(rankName);
        if (rank == null) {
            return;
        }
        
        if (slot < ITEMS_PER_PAGE) {
            // Rank item clicked
            int index = (page - 1) * ITEMS_PER_PAGE + slot;
            List<String> inheritedRanks = new ArrayList<>(rank.getInheritance());
            Collections.sort(inheritedRanks);
            
            if (index < inheritedRanks.size()) {
                String inheritedRankName = inheritedRanks.get(index);
                if (viewer.hasPermission("frizzlenperms.admin.rankinheritance")) {
                    if (plugin.getRankManager().removeInheritance(rankName, inheritedRankName, viewer)) {
                        update();
                    }
                }
            }
        } else {
            switch (slot) {
                case 45: // Previous page
                    if (page > 1) {
                        plugin.getGuiManager().openRankInheritanceGui(viewer, rankName, page - 1);
                    }
                    break;
                case 47: // Add inheritance
                    if (viewer.hasPermission("frizzlenperms.admin.rankinheritance")) {
                        // TODO: Implement rank selection GUI
                        viewer.sendMessage(MessageUtils.formatColors("&cRank selection GUI not implemented yet."));
                    }
                    break;
                case 49: // Back
                    plugin.getGuiManager().openRankInfoGui(viewer, rankName);
                    break;
                case 53: // Next page
                    int totalPages = (rank.getInheritance().size() + ITEMS_PER_PAGE - 1) / ITEMS_PER_PAGE;
                    if (page < totalPages) {
                        plugin.getGuiManager().openRankInheritanceGui(viewer, rankName, page + 1);
                    }
                    break;
            }
        }
    }
    
    @Override
    public void update() {
        initializeItems();
    }
} 