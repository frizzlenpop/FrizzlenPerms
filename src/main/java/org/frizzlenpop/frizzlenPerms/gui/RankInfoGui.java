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

import java.util.ArrayList;
import java.util.List;

/**
 * GUI for displaying rank information.
 */
public class RankInfoGui implements PermissionGui {
    
    private final FrizzlenPerms plugin;
    private final Player viewer;
    private final String rankName;
    private final Inventory inventory;
    
    /**
     * Creates a new RankInfoGui.
     *
     * @param plugin The plugin instance
     * @param viewer The player viewing the GUI
     * @param rankName The name of the rank
     */
    public RankInfoGui(FrizzlenPerms plugin, Player viewer, String rankName) {
        this.plugin = plugin;
        this.viewer = viewer;
        this.rankName = rankName;
        this.inventory = Bukkit.createInventory(null, 54, "Rank Info");
        
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
        
        // Rank info
        ItemStack infoItem = new ItemStack(Material.DIAMOND);
        ItemMeta infoMeta = infoItem.getItemMeta();
        infoMeta.setDisplayName(MessageUtils.formatColors("&e" + rank.getName()));
        List<String> infoLore = new ArrayList<>();
        infoLore.add(MessageUtils.formatColors("&7Weight: &f" + rank.getWeight()));
        if (rank.getPrefix() != null) {
            infoLore.add(MessageUtils.formatColors("&7Prefix: &f" + rank.getPrefix()));
        }
        if (rank.getSuffix() != null) {
            infoLore.add(MessageUtils.formatColors("&7Suffix: &f" + rank.getSuffix()));
        }
        if (rank.getDisplayName() != null) {
            infoLore.add(MessageUtils.formatColors("&7Display Name: &f" + rank.getDisplayName()));
        }
        if (rank.isDefault()) {
            infoLore.add(MessageUtils.formatColors("&7Default: &aYes"));
        }
        infoMeta.setLore(infoLore);
        infoItem.setItemMeta(infoMeta);
        inventory.setItem(4, infoItem);
        
        // Permissions button
        ItemStack permissionsItem = new ItemStack(Material.BOOK);
        ItemMeta permissionsMeta = permissionsItem.getItemMeta();
        permissionsMeta.setDisplayName(MessageUtils.formatColors("&ePermissions"));
        List<String> permissionsLore = new ArrayList<>();
        permissionsLore.add(MessageUtils.formatColors("&7Click to view permissions"));
        permissionsMeta.setLore(permissionsLore);
        permissionsItem.setItemMeta(permissionsMeta);
        inventory.setItem(19, permissionsItem);
        
        // Inheritance button
        ItemStack inheritanceItem = new ItemStack(Material.GOLD_INGOT);
        ItemMeta inheritanceMeta = inheritanceItem.getItemMeta();
        inheritanceMeta.setDisplayName(MessageUtils.formatColors("&eInheritance"));
        List<String> inheritanceLore = new ArrayList<>();
        inheritanceLore.add(MessageUtils.formatColors("&7Click to view inherited ranks"));
        inheritanceMeta.setLore(inheritanceLore);
        inheritanceItem.setItemMeta(inheritanceMeta);
        inventory.setItem(21, inheritanceItem);
        
        // Edit button (if has permission)
        if (viewer.hasPermission("frizzlenperms.admin.editrank")) {
            ItemStack editItem = new ItemStack(Material.ANVIL);
            ItemMeta editMeta = editItem.getItemMeta();
            editMeta.setDisplayName(MessageUtils.formatColors("&eEdit Rank"));
            List<String> editLore = new ArrayList<>();
            editLore.add(MessageUtils.formatColors("&7Click to edit rank settings"));
            editMeta.setLore(editLore);
            editItem.setItemMeta(editMeta);
            inventory.setItem(23, editItem);
        }
        
        // Back button
        ItemStack backItem = new ItemStack(Material.ARROW);
        ItemMeta backMeta = backItem.getItemMeta();
        backMeta.setDisplayName(MessageUtils.formatColors("&eBack"));
        backItem.setItemMeta(backMeta);
        inventory.setItem(49, backItem);
    }
    
    @Override
    public Inventory getInventory() {
        return inventory;
    }
    
    @Override
    public void handleClick(int slot, ClickType clickType) {
        switch (slot) {
            case 19: // Permissions
                plugin.getGuiManager().openRankPermissionsGui(viewer, rankName, 1);
                break;
            case 21: // Inheritance
                plugin.getGuiManager().openRankInheritanceGui(viewer, rankName, 1);
                break;
            case 23: // Edit
                if (viewer.hasPermission("frizzlenperms.admin.editrank")) {
                    // TODO: Implement rank edit GUI
                    viewer.sendMessage(MessageUtils.formatColors("&cRank editing GUI not implemented yet."));
                }
                break;
            case 49: // Back
                plugin.getGuiManager().openRankManagementGui(viewer, 1);
                break;
        }
    }
    
    @Override
    public void update() {
        initializeItems();
    }
} 