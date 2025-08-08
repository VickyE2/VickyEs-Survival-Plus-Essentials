package org.vicky.vspe.platform.systems.dimension.terrasupporteddimensions.dimensions.Test.Biomes;

import org.vicky.platform.world.PlatformBlockState;
import org.vicky.platform.world.PlatformMaterial;
import org.vicky.vspe.BiomeCategory;
import org.vicky.vspe.platform.systems.dimension.terrasupporteddimensions.dimensions.Test.Features.TestFeature;
import org.vicky.vspe.platform.systems.dimension.terrasupporteddimensions.Generator.utils.Biome.BaseBiome;
import org.vicky.vspe.platform.systems.dimension.terrasupporteddimensions.Generator.utils.Biome.extend.Extendibles;
import org.vicky.vspe.platform.systems.dimension.terrasupporteddimensions.Generator.utils.Biome.type.Precipitation;
import org.vicky.vspe.platform.systems.dimension.terrasupporteddimensions.Generator.utils.Biome.type.subEnums.Coasts;
import org.vicky.vspe.platform.systems.dimension.terrasupporteddimensions.Generator.utils.Colorable;
import org.vicky.vspe.platform.systems.dimension.terrasupporteddimensions.Generator.utils.Feature.Featureable;
import org.vicky.vspe.platform.systems.dimension.terrasupporteddimensions.Generator.utils.Palette.Palette;
import org.vicky.vspe.platform.systems.dimension.terrasupporteddimensions.Generator.utils.Rarity;
import org.vicky.vspe.platform.systems.dimension.vspeChunkGenerator.SimpleBlockState;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TestBiome extends BaseBiome {
    public TestBiome() {
        super("TEST_BIOME", 279620, BiomeCategory.PLAINS, Coasts.COAST_SMALL_POLAR, new Precipitation(0.6F, Precipitation.PrecipitaionType.RAIN), Rarity.VERY_COMMON);
        this.addColor(Colorable.WATER, Integer.valueOf(8947848));
        this.addExtendibles(Extendibles.EQ_ERODED_COAST);
        Palette palette = new Palette("Test_Palette");
        Map<PlatformMaterial, Integer> paletteMap = new HashMap<>();
        paletteMap.put(SimpleBlockState.Companion.from("minecraft:sand", (it) -> it).getMaterial(), 319);
        paletteMap.put(SimpleBlockState.Companion.from("minecraft:dirt", (it) -> it).getMaterial(), 30);
        palette.addLayer(paletteMap, 50);
        this.addPalettes(palette, 319);
        this.addColor(Colorable.WATER_FOG, Integer.valueOf(16777215));
        this.addFeaturesToParam(List.of(new TestFeature()), Featureable.LANDFORMS);
    }
}
