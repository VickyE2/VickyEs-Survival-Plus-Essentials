package org.vicky.vspe.systems.dimension.Generator.utils;

public enum Rarity {
    VERY_COMMON(20),
    COMMON(18),
    RARE(15),
    EPIC(12),
    LEGENDARY(9),
    MYTHIC(6),
    GOD_TIER(2);

    public final int rarityValue;

    Rarity(int rarityValue) {
        this.rarityValue = rarityValue;
    }

    public int getRarityValue() {
        return this.rarityValue;
    }
}
