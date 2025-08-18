package org.vicky.vspe.platform.features.CharmsAndTrinkets;

import org.vicky.platform.PlatformItem;

/**
 * A simple container to hold an equipped trinket and the inventory slot it was placed in.
 */
public record EquippedTrinket(PlatformItem item, int slot) {
    public PlatformItem getItem() {
        return item;
    }

    public int getSlot() {
        return slot;
    }
}