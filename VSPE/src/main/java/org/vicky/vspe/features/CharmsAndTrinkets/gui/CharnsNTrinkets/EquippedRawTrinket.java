package org.vicky.vspe.features.CharmsAndTrinkets.gui.CharnsNTrinkets;

import org.bukkit.inventory.ItemStack;
import org.vicky.guiparent.GuiCreator;

public record EquippedRawTrinket(GuiCreator.ItemConfig item, int slot) {
    public GuiCreator.ItemConfig getItem() {
        return item;
    }

    public int getSlot() {
        return slot;
    }
}
