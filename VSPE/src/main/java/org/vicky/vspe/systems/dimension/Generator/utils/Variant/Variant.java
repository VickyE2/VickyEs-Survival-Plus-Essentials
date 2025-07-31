package org.vicky.vspe.systems.dimension.Generator.utils.Variant;

import org.vicky.vspe.systems.dimension.Generator.utils.Rarity;

public interface Variant {
    String getVariantName();

    Rarity getVariantRarity();

    BiomeVariant getVariantOf();

    ClimateVariant getClimateVariantOf();
}
