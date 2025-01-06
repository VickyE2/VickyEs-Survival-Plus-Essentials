package org.vicky.vspe.systems.Dimension.Dimensions.Test.Biomes;

import org.bukkit.Material;
import org.bukkit.block.Biome;
import org.vicky.vspe.systems.Dimension.Dimensions.Test.Features.TestFeature;
import org.vicky.vspe.systems.Dimension.Generator.utils.Biome.BaseBiome;
import org.vicky.vspe.systems.Dimension.Generator.utils.Biome.extend.Extendibles;
import org.vicky.vspe.systems.Dimension.Generator.utils.Biome.extend.Tags;
import org.vicky.vspe.systems.Dimension.Generator.utils.Biome.type.Precipitation;
import org.vicky.vspe.systems.Dimension.Generator.utils.Biome.type.subEnums.Flat;
import org.vicky.vspe.systems.Dimension.Generator.utils.Biome.type.subEnums.Hills_Small;
import org.vicky.vspe.systems.Dimension.Generator.utils.Biome.type.subEnums.Mountains_Large;
import org.vicky.vspe.systems.Dimension.Generator.utils.Colorable;
import org.vicky.vspe.systems.Dimension.Generator.utils.Feature.Featureable;
import org.vicky.vspe.systems.Dimension.Generator.utils.Palette.Palette;
import org.vicky.vspe.systems.Dimension.Generator.utils.Rarity;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TestBiome4 extends BaseBiome {
    public TestBiome4() {
        super(
                "TEST_BIOME_TWO",
                0x555555,
                Biome.TAIGA,
                Mountains_Large.TROPICAL,
                new Precipitation(0.8f, Precipitation.PrecipitaionType.RAIN),
                Rarity.COMMON
        );

        addTag(Tags.LAND_CAVES);
        addColor(Colorable.WATER, 0x888888);
        addExtendibles(Extendibles.EQ_MOUNTAINS);
        Palette palette = new Palette("Test_Palette_four");
        Map<Material, Integer> paletteMap = new HashMap<>();
        paletteMap.put(Material.DIRT, 319);
        paletteMap.put(Material.STONE, 210);
        palette.addLayer(paletteMap, 50);
        addPalettes(palette, 319);
        addColor(Colorable.WATER_FOG, 0xFFFFFF);
        addFeaturesToParam(List.of(new TestFeature()), Featureable.LANDFORMS);
    }
}
