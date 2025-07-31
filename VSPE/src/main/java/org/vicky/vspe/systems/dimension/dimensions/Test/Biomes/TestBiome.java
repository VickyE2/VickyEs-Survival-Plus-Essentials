package org.vicky.vspe.systems.dimension.dimensions.Test.Biomes;

import org.bukkit.Material;
import org.bukkit.block.Biome;
import org.vicky.vspe.systems.dimension.dimensions.Test.Features.TestFeature;
import org.vicky.vspe.systems.dimension.Generator.utils.Biome.BaseBiome;
import org.vicky.vspe.systems.dimension.Generator.utils.Biome.extend.Extendibles;
import org.vicky.vspe.systems.dimension.Generator.utils.Biome.type.Precipitation;
import org.vicky.vspe.systems.dimension.Generator.utils.Biome.type.subEnums.Coasts;
import org.vicky.vspe.systems.dimension.Generator.utils.Colorable;
import org.vicky.vspe.systems.dimension.Generator.utils.Feature.Featureable;
import org.vicky.vspe.systems.dimension.Generator.utils.Palette.Palette;
import org.vicky.vspe.systems.dimension.Generator.utils.Rarity;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TestBiome extends BaseBiome {
    public TestBiome() {
        super("TEST_BIOME", 279620, Biome.PLAINS, Coasts.COAST_SMALL_POLAR, new Precipitation(0.6F, Precipitation.PrecipitaionType.RAIN), Rarity.VERY_COMMON);
        this.addColor(Colorable.WATER, Integer.valueOf(8947848));
        this.addExtendibles(Extendibles.EQ_ERODED_COAST);
        Palette palette = new Palette("Test_Palette");
        Map<Material, Integer> paletteMap = new HashMap<>();
        paletteMap.put(Material.SAND, 319);
        paletteMap.put(Material.DIRT, 30);
        palette.addLayer(paletteMap, 50);
        this.addPalettes(palette, 319);
        this.addColor(Colorable.WATER_FOG, Integer.valueOf(16777215));
        this.addFeaturesToParam(List.of(new TestFeature()), Featureable.LANDFORMS);
    }
}
