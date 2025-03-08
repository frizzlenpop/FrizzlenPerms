package org.frizzlenpop.frizzlenPerms.gui;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.frizzlenpop.frizzlenPerms.FrizzlenPerms;
import org.frizzlenpop.frizzlenPerms.models.PlayerData;
import org.frizzlenpop.frizzlenPerms.models.Rank;
import org.frizzlenpop.frizzlenPerms.utils.MessageUtils;

import java.util.*;

/**
 * GUI for displaying player information.
 */
public class PlayerInfoGui implements PermissionGui {
    
    private final FrizzlenPerms plugin;
    private final Player viewer;
    private final UUID targetUuid;
    private final Inventory inventory;
    
    /**
     * Creates a new PlayerInfoGui.
     *
     * @param plugin The plugin instance
     * @param viewer The player viewing the GUI
     * @param targetUuid The UUID of the target player
     */
    public PlayerInfoGui(FrizzlenPerms plugin, Player viewer, UUID targetUuid) {
        this.plugin = plugin;
        this.viewer = viewer;
        this.targetUuid = targetUuid;
        this.inventory = Bukkit.createInventory(null, 54, "Player Info");
        
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
        
        // Player head
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta headMeta = (SkullMeta) head.getItemMeta();
        headMeta.setOwningPlayer(Bukkit.getOfflinePlayer(targetUuid));
        headMeta.setDisplayName(MessageUtils.formatColors("&e" + playerData.getPlayerName()));
        List<String> headLore = new ArrayList<>();
        headLore.add(MessageUtils.formatColors("&7UUID: &f" + targetUuid));
        headLore.add(MessageUtils.formatColors("&7Last Login: &f" + new Date(playerData.getLastLogin())));
        headLore.add(MessageUtils.formatColors("&7Last Seen: &f" + new Date(playerData.getLastSeen())));
        headMeta.setLore(headLore);
        head.setItemMeta(headMeta);
        inventory.setItem(4, head);
        
        // Primary rank
        Rank primaryRank = plugin.getRankManager().getRank(playerData.getPrimaryRank());
        if (primaryRank != null) {
            ItemStack rankItem = new ItemStack(Material.DIAMOND);
            ItemMeta rankMeta = rankItem.getItemMeta();
            rankMeta.setDisplayName(MessageUtils.formatColors("&ePrimary Rank"));
            List<String> rankLore = new ArrayList<>();
            rankLore.add(MessageUtils.formatColors("&7Name: &f" + primaryRank.getName()));
            rankLore.add(MessageUtils.formatColors("&7Weight: &f" + primaryRank.getWeight()));
            if (primaryRank.getPrefix() != null) {
                rankLore.add(MessageUtils.formatColors("&7Prefix: &f" + primaryRank.getPrefix()));
            }
            if (primaryRank.getSuffix() != null) {
                rankLore.add(MessageUtils.formatColors("&7Suffix: &f" + primaryRank.getSuffix()));
            }
            rankMeta.setLore(rankLore);
            rankItem.setItemMeta(rankMeta);
            inventory.setItem(19, rankItem);
        }
        
        // Secondary ranks button
        ItemStack secondaryRanksItem = new ItemStack(Material.GOLD_INGOT);
        ItemMeta secondaryRanksMeta = secondaryRanksItem.getItemMeta();
        secondaryRanksMeta.setDisplayName(MessageUtils.formatColors("&eSecondary Ranks"));
        List<String> secondaryRanksLore = new ArrayList<>();
        secondaryRanksLore.add(MessageUtils.formatColors("&7Click to view secondary ranks"));
        secondaryRanksMeta.setLore(secondaryRanksLore);
        secondaryRanksItem.setItemMeta(secondaryRanksMeta);
        inventory.setItem(21, secondaryRanksItem);
        
        // Permissions button
        ItemStack permissionsItem = new ItemStack(Material.BOOK);
        ItemMeta permissionsMeta = permissionsItem.getItemMeta();
        permissionsMeta.setDisplayName(MessageUtils.formatColors("&ePermissions"));
        List<String> permissionsLore = new ArrayList<>();
        permissionsLore.add(MessageUtils.formatColors("&7Click to view permissions"));
        permissionsMeta.setLore(permissionsLore);
        permissionsItem.setItemMeta(permissionsMeta);
        inventory.setItem(23, permissionsItem);
        
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
            case 21: // Secondary ranks
                plugin.getGuiManager().openPlayerRanksGui(viewer, targetUuid, 1);
                break;
            case 23: // Permissions
                plugin.getGuiManager().openPlayerPermissionsGui(viewer, targetUuid, 1);
                break;
            case 49: // Back
                plugin.getGuiManager().openPlayerManagementGui(viewer, 1);
                break;
        }
    }
    
    @Override
    public void update() {
        initializeItems();
    }
} 