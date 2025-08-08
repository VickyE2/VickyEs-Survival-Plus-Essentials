package org.vicky.vspe.platform.features.CharmsAndTrinkets;

public interface PlatformTrinketSlot {
    String getSlotId(); // e.g., "curios:ring" or "paper:amulet"
    int[] getSlotIndexes(); // Optional for Paper-like systems
}
