package org.frizzlenpop.frizzlenPerms.gui;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
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
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * GUI for managing ranks.
 */
public class RankManagementGui implements PermissionGui {
    
    private final FrizzlenPerms plugin;
    private final Player player;
    private final int page;
    private final Inventory inventory;
    
    private static final int RANKS_PER_PAGE = 36;
    
    /**
     * Creates a new RankManagementGui.
     *
     * @param plugin The plugin instance
     * @param player The player viewing the GUI
     * @param page The page number
     */
    public RankManagementGui(FrizzlenPerms plugin, Player player, int page) {
        this.plugin = plugin;
        this.player = player;
        this.page = Math.max(1, page);
        
        // Create inventory
        String title = MessageUtils.formatColors("&8[&6FrizzlenPerms&8] &fRanks (Page " + this.page + ")");
        this.inventory = Bukkit.createInventory(null, 54, title);
        
        // Initialize items
        initializeItems();
    }
    
    /**
     * Initializes the items in the GUI.
     */
    private void initializeItems() {
        // Get all ranks
        List<Rank> allRanks = plugin.getRankManager().getAllRanks();
        
        // Sort by weight (highest first)
        allRanks.sort(Comparator.comparingInt(Rank::getWeight).reversed());
        
        // Calculate total pages
        int totalPages = (int) Math.ceil((double) allRanks.size() / RANKS_PER_PAGE);
        
        // Calculate start and end indices
        int startIndex = (page - 1) * RANKS_PER_PAGE;
        int endIndex = Math.min(startIndex + RANKS_PER_PAGE, allRanks.size());
        
        // Add rank items
        for (int i = startIndex; i < endIndex; i++) {
            Rank rank = allRanks.get(i);
            String rankName = rank.getName();
            String displayName = rank.getDisplayName();
            int weight = rank.getWeight();
            
            Material material = Material.GOLDEN_HELMET;
            if (rankName.equalsIgnoreCase(plugin.getConfigManager().getDefaultRankName())) {
                material = Material.LEATHER_HELMET;
            } else if (rankName.equalsIgnoreCase(plugin.getConfigManager().getAdminRankName())) {
                material = Material.DIAMOND_HELMET;
            }
            
            ItemStack rankItem = new ItemStack(material);
            ItemMeta meta = rankItem.getItemMeta();
            
            // Set display name
            meta.setDisplayName(MessageUtils.formatColors(rank.getColor() + displayName));
            
            // Set lore
            List<String> lore = new ArrayList<>();
            lore.add(MessageUtils.formatColors("&7Name: &f" + rankName));
            lore.add(MessageUtils.formatColors("&7Weight: &f" + weight));
            
            // Add inheritance
            List<String> inheritance = rank.getInheritance();
            if (!inheritance.isEmpty()) {
                lore.add(MessageUtils.formatColors("&7Inherits: &f" + String.join(", ", inheritance)));
            }
            
            // Add permissions count
            int permCount = rank.getPermissions().size();
            lore.add(MessageUtils.formatColors("&7Permissions: &f" + permCount));
            
            // Add default rank indicator
            if (rankName.equalsIgnoreCase(plugin.getConfigManager().getDefaultRankName())) {
                lore.add("");
                lore.add(MessageUtils.formatColors("&a&lDefault Rank"));
            }
            
            // Add click instructions
            lore.add("");
            lore.add(MessageUtils.formatColors("&eLeft-click to view rank info"));
            lore.add(MessageUtils.formatColors("&eRight-click to manage permissions"));
            lore.add(MessageUtils.formatColors("&eShift-click to manage inheritance"));
            
            meta.setLore(lore);
            rankItem.setItemMeta(meta);
            
            // Add to inventory
            inventory.setItem(i - startIndex, rankItem);
        }
        
        // Add create rank button
        ItemStack createButton = new ItemStack(Material.EMERALD);
        ItemMeta createMeta = createButton.getItemMeta();
        createMeta.setDisplayName(MessageUtils.formatColors("&a&lCreate New Rank"));
        List<String> createLore = new ArrayList<>();
        createLore.add(MessageUtils.formatColors("&7Click to create a new rank"));
        createMeta.setLore(createLore);
        createButton.setItemMeta(createMeta);
        inventory.setItem(47, createButton);
        
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
        // Check if the slot is a rank slot
        if (slot >= 0 && slot < RANKS_PER_PAGE) {
            // Calculate the index in the rank list
            int rankIndex = (page - 1) * RANKS_PER_PAGE + slot;
            List<Rank> allRanks = plugin.getRankManager().getAllRanks();
            
            // Sort by weight (highest first)
            allRanks.sort(Comparator.comparingInt(Rank::getWeight).reversed());
            
            // Check if the index is valid
            if (rankIndex < allRanks.size()) {
                Rank rank = allRanks.get(rankIndex);
                String rankName = rank.getName();
                
                // Handle different click types
                if (clickType == ClickType.LEFT) {
                    // View rank info
                    plugin.getGuiManager().openRankInfoGui(player, rankName);
                } else if (clickType == ClickType.RIGHT) {
                    // Manage permissions
                    plugin.getGuiManager().openRankPermissionsGui(player, rankName, 1);
                } else if (clickType == ClickType.SHIFT_LEFT || clickType == ClickType.SHIFT_RIGHT) {
                    // Manage inheritance
                    plugin.getGuiManager().openRankInheritanceGui(player, rankName, 1);
                }
            }
        } else if (slot == 45 && page > 1) {
            // Previous page
            plugin.getGuiManager().openRankManagementGui(player, page - 1);
        } else if (slot == 47) {
            // Create new rank
            player.closeInventory();
            player.sendMessage(MessageUtils.formatColors("&aUse &e/frizzlenperms rankcreate <name> [displayName] [weight] [color] &ato create a new rank."));
        } else if (slot == 53) {
            // Next page
            List<Rank> allRanks = plugin.getRankManager().getAllRanks();
            int totalPages = (int) Math.ceil((double) allRanks.size() / RANKS_PER_PAGE);
            
            if (page < totalPages) {
                plugin.getGuiManager().openRankManagementGui(player, page + 1);
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