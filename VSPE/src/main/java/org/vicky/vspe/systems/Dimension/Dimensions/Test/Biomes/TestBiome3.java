package org.vicky.vspe.systems.Dimension.Dimensions.Test.Biomes;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.bukkit.Material;
import org.bukkit.block.Biome;
import org.vicky.vspe.systems.Dimension.Dimensions.Test.Features.TestFeature;
import org.vicky.vspe.systems.Dimension.Generator.utils.Colorable;
import org.vicky.vspe.systems.Dimension.Generator.utils.Rarity;
import org.vicky.vspe.systems.Dimension.Generator.utils.Biome.BaseBiome;
import org.vicky.vspe.systems.Dimension.Generator.utils.Biome.extend.Extendibles;
import org.vicky.vspe.systems.Dimension.Generator.utils.Biome.type.Precipitation;
import org.vicky.vspe.systems.Dimension.Generator.utils.Biome.type.subEnums.Ocean_Flat;
import org.vicky.vspe.systems.Dimension.Generator.utils.Feature.Featureable;
import org.vicky.vspe.systems.Dimension.Generator.utils.Palette.Palette;

public class TestBiome3 extends BaseBiome {
   public TestBiome3() {
      super("TEST_BIOME_three", 5592405, Biome.COLD_OCEAN, Ocean_Flat.POLAR, new Precipitation(0.8F, Precipitation.PrecipitaionType.RAIN), Rarity.COMMON);
      this.addColor(Colorable.WATER, Integer.valueOf(8947848));
      this.addExtendibles(new Extendibles[]{Extendibles.EQ_GLOBAL_OCEAN});
      Palette palette = new Palette("Test_Palette_three");
      Map<Material, Integer> paletteMap = new HashMap<>();
      paletteMap.put(Material.ANDESITE, 319);
      paletteMap.put(Material.DIRT, 318);
      paletteMap.put(Material.DEEPSLATE, 30);
      palette.addLayer(paletteMap, 50);
      this.addPalettes(palette, 319);
      this.addColor(Colorable.WATER_FOG, Integer.valueOf(16777215));
      this.addFeaturesToParam(List.of(new TestFeature()), Featureable.LANDFORMS);
   }
}
