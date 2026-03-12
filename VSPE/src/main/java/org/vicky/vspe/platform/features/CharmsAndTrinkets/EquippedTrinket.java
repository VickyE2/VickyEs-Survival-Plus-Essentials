package org.vicky.vspe.platform.features.CharmsAndTrinkets;

import org.vicky.platform.PlatformItemStack;

/**
 * A simple container to hold an equipped trinket and the inventory slot it was placed in.
 */
public record EquippedTrinket(PlatformItemStack item, int slot) {
    public PlatformItemStack getItem() {
        return item;
    }

    public int getSlot() {
        return slot;
    }
}