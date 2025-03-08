package org.frizzlenpop.frizzlenPerms.gui;

import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;

/**
 * Interface for all permission GUI classes.
 */
public interface PermissionGui {
    
    /**
     * Gets the inventory for this GUI.
     *
     * @return The inventory
     */
    Inventory getInventory();
    
    /**
     * Handles a click in the GUI.
     *
     * @param slot The slot that was clicked
     * @param clickType The type of click
     */
    void handleClick(int slot, ClickType clickType);
    
    /**
     * Updates the GUI.
     */
    void update();
} 