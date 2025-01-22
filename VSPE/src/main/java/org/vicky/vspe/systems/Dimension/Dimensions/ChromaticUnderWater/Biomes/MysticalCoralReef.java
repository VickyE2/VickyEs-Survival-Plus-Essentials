package org.vicky.vspe.systems.Dimension.Dimensions.ChromaticUnderWater.Biomes;

import org.bukkit.block.Biome;
import org.vicky.vspe.systems.Dimension.Dimensions.ChromaticUnderWater.Palettes.MossOceanFloor;
import org.vicky.vspe.systems.Dimension.Generator.utils.Colorable;
import org.vicky.vspe.systems.Dimension.Generator.utils.Rarity;
import org.vicky.vspe.systems.Dimension.Generator.utils.Biome.BaseBiome;
import org.vicky.vspe.systems.Dimension.Generator.utils.Biome.extend.Extendibles;
import org.vicky.vspe.systems.Dimension.Generator.utils.Biome.extend.Tags;
import org.vicky.vspe.systems.Dimension.Generator.utils.Biome.type.Precipitation;
import org.vicky.vspe.systems.Dimension.Generator.utils.Biome.type.subEnums.Ocean_Flat;

public class MysticalCoralReef extends BaseBiome {
   public MysticalCoralReef() {
      super(
         "MYSTICAL_CORAL_REEF", 4909768, Biome.WARM_OCEAN, Ocean_Flat.TROPICAL, new Precipitation(0.7F, Precipitation.PrecipitaionType.RAIN), Rarity.LEGENDARY
      );
      this.setVariantOf(new CoralReefs());
      this.addTag(Tags.USE_UNDERWATER_RAVINE);
      this.addColor(Colorable.WATER, Integer.valueOf(6158037));
      this.addColor(Colorable.WATER_FOG, Integer.valueOf(93781794));
      this.addExtendible(Extendibles.ENVIRONMENT_MARINE_OCEAN);
      this.addExtendible(Extendibles.EQ_FLAT);
      this.addPalettes(new MossOceanFloor(), 319);
   }
}
