package org.vicky.vspe.systems.Dimension.Dimensions.Test.Biomes;

import org.bukkit.Material;
import org.bukkit.block.Biome;
import org.vicky.vspe.systems.Dimension.Dimensions.Test.Features.TestFeature;
import org.vicky.vspe.systems.Dimension.Generator.utils.Biome.BaseBiome;
import org.vicky.vspe.systems.Dimension.Generator.utils.Biome.extend.Extendibles;
import org.vicky.vspe.systems.Dimension.Generator.utils.Biome.type.Precipitation;
import org.vicky.vspe.systems.Dimension.Generator.utils.Biome.type.subEnums.Coasts;
import org.vicky.vspe.systems.Dimension.Generator.utils.Biome.type.subEnums.Hills_Small;
import org.vicky.vspe.systems.Dimension.Generator.utils.Colorable;
import org.vicky.vspe.systems.Dimension.Generator.utils.Feature.Featureable;
import org.vicky.vspe.systems.Dimension.Generator.utils.Palette.Palette;
import org.vicky.vspe.systems.Dimension.Generator.utils.Rarity;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TestBiome extends BaseBiome {
    public TestBiome() {
        super("TEST_BIOME", 0x44444, Biome.PLAINS, Coasts.COAST_SMALL_POLAR, new Precipitation(0.6f, Precipitation.PrecipitaionType.RAIN), Rarity.VERY_COMMON);

        addColor(Colorable.WATER, 0x888888);
        addExtendibles(Extendibles.EQ_ERODED_COAST);
        Palette palette = new Palette("Test_Palette");
            Map<Material, Integer> paletteMap = new HashMap<>();
                paletteMap.put(Material.SAND, 319);
                paletteMap.put(Material.DIRT, 30);
        palette.addLayer(paletteMap, 50);
        addPalettes(palette, 319);
        addColor(Colorable.WATER_FOG, 0xFFFFFF);
        addFeaturesToParam(List.of(new TestFeature()), Featureable.LANDFORMS);
    }
}
