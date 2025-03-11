package org.vicky.vspe.features.CharmsAndTrinkets.gui.CharnsNTrinkets;

import org.bukkit.inventory.ItemStack;

/**
 * A simple container to hold an equipped trinket and the inventory slot it was placed in.
 */
public record EquippedTrinket(ItemStack item, int slot) {
    public ItemStack getItem() {
        return item;
    }

    public int getSlot() {
        return slot;
    }
}
