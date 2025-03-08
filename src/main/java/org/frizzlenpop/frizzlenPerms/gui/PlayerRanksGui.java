package org.frizzlenpop.frizzlenPerms.gui;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.frizzlenpop.frizzlenPerms.FrizzlenPerms;
import org.frizzlenpop.frizzlenPerms.models.PlayerData;
import org.frizzlenpop.frizzlenPerms.models.Rank;
import org.frizzlenpop.frizzlenPerms.utils.MessageUtils;

import java.util.*;

/**
 * GUI for managing player ranks.
 */
public class PlayerRanksGui implements PermissionGui {
    
    private static final int ITEMS_PER_PAGE = 45;
    
    private final FrizzlenPerms plugin;
    private final Player viewer;
    private final UUID targetUuid;
    private final int page;
    private final Inventory inventory;
    
    /**
     * Creates a new PlayerRanksGui.
     *
     * @param plugin The plugin instance
     * @param viewer The player viewing the GUI
     * @param targetUuid The UUID of the target player
     * @param page The page number
     */
    public PlayerRanksGui(FrizzlenPerms plugin, Player viewer, UUID targetUuid, int page) {
        this.plugin = plugin;
        this.viewer = viewer;
        this.targetUuid = targetUuid;
        this.page = Math.max(1, page);
        this.inventory = Bukkit.createInventory(null, 54, "Player Ranks");
        
        initializeItems();
    }
    
    /**
     * Initializes the items in the GUI.
     */
    private void initializeItems() {
        PlayerData playerData = plugin.getDataManager().getPlayerData(targetUuid);
        if (playerData == null) {
            return;
        }
        
        // Get all secondary ranks
        List<String> ranks = new ArrayList<>(playerData.getSecondaryRanks());
        Collections.sort(ranks);
        
        // Calculate total pages
        int totalPages = (ranks.size() + ITEMS_PER_PAGE - 1) / ITEMS_PER_PAGE;
        if (totalPages == 0) totalPages = 1;
        
        // Add rank items
        int startIndex = (page - 1) * ITEMS_PER_PAGE;
        int endIndex = Math.min(startIndex + ITEMS_PER_PAGE, ranks.size());
        
        for (int i = startIndex; i < endIndex; i++) {
            String rankName = ranks.get(i);
            Rank rank = plugin.getRankManager().getRank(rankName);
            if (rank != null) {
                ItemStack item = new ItemStack(Material.GOLD_INGOT);
                ItemMeta meta = item.getItemMeta();
                meta.setDisplayName(MessageUtils.formatColors("&e" + rank.getName()));
                
                List<String> lore = new ArrayList<>();
                lore.add(MessageUtils.formatColors("&7Weight: &f" + rank.getWeight()));
                if (rank.getPrefix() != null) {
                    lore.add(MessageUtils.formatColors("&7Prefix: &f" + rank.getPrefix()));
                }
                if (rank.getSuffix() != null) {
                    lore.add(MessageUtils.formatColors("&7Suffix: &f" + rank.getSuffix()));
                }
                if (playerData.isTemporaryRank(rankName)) {
                    long expiration = playerData.getTemporaryRankExpiration(rankName);
                    lore.add(MessageUtils.formatColors("&7Expires: &f" + new Date(expiration)));
                }
                lore.add(MessageUtils.formatColors("&7Click to remove"));
                
                meta.setLore(lore);
                item.setItemMeta(meta);
                inventory.setItem(i - startIndex, item);
            }
        }
        
        // Add rank button (if has permission)
        if (viewer.hasPermission("frizzlenperms.admin.addrank")) {
            ItemStack addItem = new ItemStack(Material.EMERALD);
            ItemMeta addMeta = addItem.getItemMeta();
            addMeta.setDisplayName(MessageUtils.formatColors("&aAdd Rank"));
            List<String> addLore = new ArrayList<>();
            addLore.add(MessageUtils.formatColors("&7Click to add a rank"));
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
        PlayerData playerData = plugin.getDataManager().getPlayerData(targetUuid);
        if (playerData == null) {
            return;
        }
        
        if (slot < ITEMS_PER_PAGE) {
            // Rank item clicked
            int index = (page - 1) * ITEMS_PER_PAGE + slot;
            List<String> ranks = new ArrayList<>(playerData.getSecondaryRanks());
            Collections.sort(ranks);
            
            if (index < ranks.size()) {
                String rankName = ranks.get(index);
                if (viewer.hasPermission("frizzlenperms.admin.removerank")) {
                    playerData.getSecondaryRanks().remove(rankName);
                    plugin.getDataManager().savePlayerData(playerData);
                    
                    // Update permissions if player is online
                    Player target = Bukkit.getPlayer(targetUuid);
                    if (target != null && target.isOnline()) {
                        plugin.getPermissionManager().calculateAndApplyPermissions(target);
                    }
                    
                    update();
                }
            }
        } else {
            switch (slot) {
                case 45: // Previous page
                    if (page > 1) {
                        plugin.getGuiManager().openPlayerRanksGui(viewer, targetUuid, page - 1);
                    }
                    break;
                case 47: // Add rank
                    if (viewer.hasPermission("frizzlenperms.admin.addrank")) {
                        // TODO: Implement rank selection GUI
                        viewer.sendMessage(MessageUtils.formatColors("&cRank selection GUI not implemented yet."));
                    }
                    break;
                case 49: // Back
                    plugin.getGuiManager().openPlayerInfoGui(viewer, targetUuid);
                    break;
                case 53: // Next page
                    int totalPages = (playerData.getSecondaryRanks().size() + ITEMS_PER_PAGE - 1) / ITEMS_PER_PAGE;
                    if (page < totalPages) {
                        plugin.getGuiManager().openPlayerRanksGui(viewer, targetUuid, page + 1);
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