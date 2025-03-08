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
import org.frizzlenpop.frizzlenPerms.utils.MessageUtils;

import java.util.*;

/**
 * GUI for managing player permissions.
 */
public class PlayerPermissionsGui implements PermissionGui {
    
    private static final int ITEMS_PER_PAGE = 45;
    
    private final FrizzlenPerms plugin;
    private final Player viewer;
    private final UUID targetUuid;
    private final int page;
    private final Inventory inventory;
    
    /**
     * Creates a new PlayerPermissionsGui.
     *
     * @param plugin The plugin instance
     * @param viewer The player viewing the GUI
     * @param targetUuid The UUID of the target player
     * @param page The page number
     */
    public PlayerPermissionsGui(FrizzlenPerms plugin, Player viewer, UUID targetUuid, int page) {
        this.plugin = plugin;
        this.viewer = viewer;
        this.targetUuid = targetUuid;
        this.page = Math.max(1, page);
        this.inventory = Bukkit.createInventory(null, 54, "Player Permissions");
        
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
        
        // Get all permissions
        List<String> permissions = new ArrayList<>(playerData.getPermissions());
        Collections.sort(permissions);
        
        // Calculate total pages
        int totalPages = (permissions.size() + ITEMS_PER_PAGE - 1) / ITEMS_PER_PAGE;
        if (totalPages == 0) totalPages = 1;
        
        // Add permission items
        int startIndex = (page - 1) * ITEMS_PER_PAGE;
        int endIndex = Math.min(startIndex + ITEMS_PER_PAGE, permissions.size());
        
        for (int i = startIndex; i < endIndex; i++) {
            String permission = permissions.get(i);
            ItemStack item = new ItemStack(Material.PAPER);
            ItemMeta meta = item.getItemMeta();
            meta.setDisplayName(MessageUtils.formatColors("&f" + permission));
            
            List<String> lore = new ArrayList<>();
            if (playerData.isTemporaryPermission(permission)) {
                long expiration = playerData.getTemporaryPermissionExpiration(permission);
                lore.add(MessageUtils.formatColors("&7Expires: &f" + new Date(expiration)));
            }
            lore.add(MessageUtils.formatColors("&7Click to remove"));
            
            meta.setLore(lore);
            item.setItemMeta(meta);
            inventory.setItem(i - startIndex, item);
        }
        
        // Add permission button (if has permission)
        if (viewer.hasPermission("frizzlenperms.admin.addpermission")) {
            ItemStack addItem = new ItemStack(Material.EMERALD);
            ItemMeta addMeta = addItem.getItemMeta();
            addMeta.setDisplayName(MessageUtils.formatColors("&aAdd Permission"));
            List<String> addLore = new ArrayList<>();
            addLore.add(MessageUtils.formatColors("&7Click to add a permission"));
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
            // Permission item clicked
            int index = (page - 1) * ITEMS_PER_PAGE + slot;
            List<String> permissions = new ArrayList<>(playerData.getPermissions());
            Collections.sort(permissions);
            
            if (index < permissions.size()) {
                String permission = permissions.get(index);
                if (viewer.hasPermission("frizzlenperms.admin.removepermission")) {
                    playerData.removePermission(permission);
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
                        plugin.getGuiManager().openPlayerPermissionsGui(viewer, targetUuid, page - 1);
                    }
                    break;
                case 47: // Add permission
                    if (viewer.hasPermission("frizzlenperms.admin.addpermission")) {
                        // TODO: Implement permission input GUI
                        viewer.sendMessage(MessageUtils.formatColors("&cPermission input GUI not implemented yet."));
                    }
                    break;
                case 49: // Back
                    plugin.getGuiManager().openPlayerInfoGui(viewer, targetUuid);
                    break;
                case 53: // Next page
                    int totalPages = (playerData.getPermissions().size() + ITEMS_PER_PAGE - 1) / ITEMS_PER_PAGE;
                    if (page < totalPages) {
                        plugin.getGuiManager().openPlayerPermissionsGui(viewer, targetUuid, page + 1);
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