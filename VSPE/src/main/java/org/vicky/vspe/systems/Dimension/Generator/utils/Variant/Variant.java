package org.vicky.vspe.systems.Dimension.Generator.utils.Variant;

import org.vicky.vspe.systems.Dimension.Generator.utils.Rarity;

public interface Variant {
   String getVariantName();

   Rarity getVariantRarity();

   BiomeVariant getVariantOf();

   ClimateVariant getClimateVariantOf();
}
