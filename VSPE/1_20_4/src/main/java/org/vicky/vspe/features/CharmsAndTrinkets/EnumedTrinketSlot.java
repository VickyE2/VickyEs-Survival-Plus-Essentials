package org.vicky.vspe.features.CharmsAndTrinkets;

import org.vicky.vspe.platform.features.CharmsAndTrinkets.PlatformTrinketSlot;

public class EnumedTrinketSlot implements PlatformTrinketSlot {

    private final TrinketSlot slot;

    public EnumedTrinketSlot(TrinketSlot slot) {
        this.slot = slot;
    }

    @Override
    public String getSlotId() {
        return slot.name().toLowerCase();
    }

    @Override
    public int[] getSlotIndexes() {
        return slot.getSlots();
    }
}
