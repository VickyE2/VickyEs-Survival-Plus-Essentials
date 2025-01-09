package org.vicky.vspe.systems.Dimension.Generator.utils;

public enum Rarity {
    VERY_COMMON(6),
    COMMON(5),
    RARE(4),
    EPIC(3),
    LEGENDARY(2),
    GOD_TIER(1);

    public final int rarityValue;

    Rarity(int rarityValue) {
        this.rarityValue = rarityValue;
    }

    public int getRarityValue() {
        return rarityValue;
    }
}
