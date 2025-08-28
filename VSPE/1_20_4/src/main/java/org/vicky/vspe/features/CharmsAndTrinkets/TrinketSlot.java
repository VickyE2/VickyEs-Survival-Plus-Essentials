package org.vicky.vspe.features.CharmsAndTrinkets;

public enum TrinketSlot {
    AMULET(23),
    RING(13, 15),
    BRACELET(4, 6),
    EARRING(10, 12),
    HEAD(1),
    BELT(8),
    ANKLET(16, 18),
    CHARM(31, 33);

    private final int[] slots;

    TrinketSlot(int... slots) {
        this.slots = slots;
    }

    public int[] getSlots() {
        return slots;
    }
}
