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
import org.vicky.vspe.systems.Dimension.Generator.utils.Biome.extend.Tags;
import org.vicky.vspe.systems.Dimension.Generator.utils.Biome.type.Precipitation;
import org.vicky.vspe.systems.Dimension.Generator.utils.Biome.type.subEnums.Mountains_Large;
import org.vicky.vspe.systems.Dimension.Generator.utils.Feature.Featureable;
import org.vicky.vspe.systems.Dimension.Generator.utils.Palette.Palette;

public class TestBiome4 extends BaseBiome {
   public TestBiome4() {
      super("TEST_BIOME_TWO", 5592405, Biome.TAIGA, Mountains_Large.TROPICAL, new Precipitation(0.8F, Precipitation.PrecipitaionType.RAIN), Rarity.COMMON);
      this.addTag(Tags.LAND_CAVES);
      this.addColor(Colorable.WATER, Integer.valueOf(8947848));
      this.addExtendibles(new Extendibles[]{Extendibles.EQ_MOUNTAINS});
      Palette palette = new Palette("Test_Palette_four");
      Map<Material, Integer> paletteMap = new HashMap<>();
      paletteMap.put(Material.DIRT, 319);
      paletteMap.put(Material.STONE, 210);
      palette.addLayer(paletteMap, 50);
      this.addPalettes(palette, 319);
      this.addColor(Colorable.WATER_FOG, Integer.valueOf(16777215));
      this.addFeaturesToParam(List.of(new TestFeature()), Featureable.LANDFORMS);
   }
}
