package org.vicky.vspe.platform.systems.dimension.terrasupporteddimensions.Generator.utils.Variant;

import org.vicky.vspe.platform.systems.dimension.terrasupporteddimensions.Generator.utils.Rarity;

public interface Variant {
    String getVariantName();

    Rarity getVariantRarity();

    BiomeVariant getVariantOf();

    ClimateVariant getClimateVariantOf();
}
