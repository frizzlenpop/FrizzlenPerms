package org.frizzlenpop.frizzlenPerms.gui;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.frizzlenpop.frizzlenPerms.FrizzlenPerms;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Manages GUI creation and interaction.
 */
public class GuiManager implements Listener {
    
    private final FrizzlenPerms plugin;
    private final Map<UUID, PermissionGui> openGuis;
    
    /**
     * Creates a new GuiManager.
     *
     * @param plugin The plugin instance
     */
    public GuiManager(FrizzlenPerms plugin) {
        this.plugin = plugin;
        this.openGuis = new HashMap<>();
    }
    
    /**
     * Opens the main GUI for a player.
     *
     * @param player The player
     */
    public void openMainGui(Player player) {
        MainGui gui = new MainGui(plugin, player);
        openGui(player, gui);
    }
    
    /**
     * Opens the player management GUI for a player.
     *
     * @param player The player viewing the GUI
     * @param page The page number
     */
    public void openPlayerManagementGui(Player player, int page) {
        PlayerManagementGui gui = new PlayerManagementGui(plugin, player, page);
        openGui(player, gui);
    }
    
    /**
     * Opens the rank management GUI for a player.
     *
     * @param player The player viewing the GUI
     * @param page The page number
     */
    public void openRankManagementGui(Player player, int page) {
        RankManagementGui gui = new RankManagementGui(plugin, player, page);
        openGui(player, gui);
    }
    
    /**
     * Opens the player info GUI for a player.
     *
     * @param player The player viewing the GUI
     * @param targetUuid The UUID of the target player
     */
    public void openPlayerInfoGui(Player player, UUID targetUuid) {
        PlayerInfoGui gui = new PlayerInfoGui(plugin, player, targetUuid);
        openGui(player, gui);
    }
    
    /**
     * Opens the rank info GUI for a player.
     *
     * @param player The player viewing the GUI
     * @param rankName The name of the rank
     */
    public void openRankInfoGui(Player player, String rankName) {
        RankInfoGui gui = new RankInfoGui(plugin, player, rankName);
        openGui(player, gui);
    }
    
    /**
     * Opens the player permissions GUI for a player.
     *
     * @param player The player viewing the GUI
     * @param targetUuid The UUID of the target player
     * @param page The page number
     */
    public void openPlayerPermissionsGui(Player player, UUID targetUuid, int page) {
        PlayerPermissionsGui gui = new PlayerPermissionsGui(plugin, player, targetUuid, page);
        openGui(player, gui);
    }
    
    /**
     * Opens the rank permissions GUI for a player.
     *
     * @param player The player viewing the GUI
     * @param rankName The name of the rank
     * @param page The page number
     */
    public void openRankPermissionsGui(Player player, String rankName, int page) {
        RankPermissionsGui gui = new RankPermissionsGui(plugin, player, rankName, page);
        openGui(player, gui);
    }
    
    /**
     * Opens the player ranks GUI for a player.
     *
     * @param player The player viewing the GUI
     * @param targetUuid The UUID of the target player
     * @param page The page number
     */
    public void openPlayerRanksGui(Player player, UUID targetUuid, int page) {
        PlayerRanksGui gui = new PlayerRanksGui(plugin, player, targetUuid, page);
        openGui(player, gui);
    }
    
    /**
     * Opens the rank inheritance GUI for a player.
     *
     * @param player The player viewing the GUI
     * @param rankName The name of the rank
     * @param page The page number
     */
    public void openRankInheritanceGui(Player player, String rankName, int page) {
        RankInheritanceGui gui = new RankInheritanceGui(plugin, player, rankName, page);
        openGui(player, gui);
    }
    
    /**
     * Opens a GUI for a player.
     *
     * @param player The player
     * @param gui The GUI to open
     */
    private void openGui(Player player, PermissionGui gui) {
        // Close any open GUI
        if (openGuis.containsKey(player.getUniqueId())) {
            player.closeInventory();
        }
        
        // Open the new GUI
        Inventory inventory = gui.getInventory();
        player.openInventory(inventory);
        
        // Store the GUI
        openGuis.put(player.getUniqueId(), gui);
    }
    
    /**
     * Handles inventory click events.
     *
     * @param event The event
     */
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }
        
        Player player = (Player) event.getWhoClicked();
        UUID playerUuid = player.getUniqueId();
        
        // Check if the player has a GUI open
        if (!openGuis.containsKey(playerUuid)) {
            return;
        }
        
        // Check if the click was in the GUI
        if (event.getClickedInventory() == null || !event.getView().getTopInventory().equals(event.getClickedInventory())) {
            return;
        }
        
        // Cancel the event
        event.setCancelled(true);
        
        // Handle the click
        PermissionGui gui = openGuis.get(playerUuid);
        gui.handleClick(event.getSlot(), event.getClick());
    }
    
    /**
     * Handles inventory close events.
     *
     * @param event The event
     */
    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player)) {
            return;
        }
        
        Player player = (Player) event.getPlayer();
        UUID playerUuid = player.getUniqueId();
        
        // Remove the GUI from the map
        openGuis.remove(playerUuid);
    }
    
    /**
     * Closes all open GUIs.
     */
    public void closeAllGuis() {
        for (UUID playerUuid : openGuis.keySet()) {
            Player player = Bukkit.getPlayer(playerUuid);
            if (player != null && player.isOnline()) {
                player.closeInventory();
            }
        }
        
        openGuis.clear();
    }
} 