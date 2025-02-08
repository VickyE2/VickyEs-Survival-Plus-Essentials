package org.vicky.vspe.systems.Dimension.Dimensions.ChromaticUnderWater.Biomes;

import org.bukkit.block.Biome;
import org.vicky.vspe.systems.Dimension.Dimensions.ChromaticUnderWater.Features.SeagrassFloor;
import org.vicky.vspe.systems.Dimension.Dimensions.ChromaticUnderWater.Palettes.SandyOceanFloor;
import org.vicky.vspe.systems.Dimension.Generator.utils.Biome.BaseBiome;
import org.vicky.vspe.systems.Dimension.Generator.utils.Biome.extend.Extendibles;
import org.vicky.vspe.systems.Dimension.Generator.utils.Biome.extend.Tags;
import org.vicky.vspe.systems.Dimension.Generator.utils.Biome.type.Precipitation;
import org.vicky.vspe.systems.Dimension.Generator.utils.Biome.type.subEnums.Ocean_Flat;
import org.vicky.vspe.systems.Dimension.Generator.utils.Colorable;
import org.vicky.vspe.systems.Dimension.Generator.utils.Feature.Featureable;
import org.vicky.vspe.systems.Dimension.Generator.utils.Rarity;
import org.vicky.vspe.systems.Dimension.Generator.utils.Variant.BiomeVariant;
import org.vicky.vspe.systems.Dimension.Generator.utils.Variant.ClimateVariant;
import org.vicky.vspe.systems.Dimension.Generator.utils.Variant.Variant;

import java.util.List;

public class BlueSeagrassMeadows extends BaseBiome implements Variant {
    public BlueSeagrassMeadows() {
        super(
                "BLUE_SEAGRASS_MEADOWS",
                0x334455,
                Biome.WARM_OCEAN,
                Ocean_Flat.BOREAL,
                new Precipitation(6, Precipitation.PrecipitaionType.SNOW),
                Rarity.COMMON
        );
        this.addTag(Tags.USE_UNDERWATER_RAVINE);
        this.addColor(Colorable.WATER, 0x414dba);
        this.addColor(Colorable.WATER_FOG, 0x414dba);
        this.addColor(Colorable.GRASS, 0x414dba);
        this.addColor(Colorable.FOLIAGE, 0x414dba);
        this.addPalettes(new SandyOceanFloor(), 319);
        addFeaturesToParam(List.of(new SeagrassFloor()), Featureable.UNDERWATER_FLORA);
        addExtendible(Extendibles.EQ_GLOBAL_OCEAN);
        isOcean();
    }

    @Override
    public Rarity getVariantRarity() {
        return Rarity.RARE;
    }

    @Override
    public BiomeVariant getVariantOf() {
        return new SeagrassMeadows();
    }

    @Override
    public ClimateVariant getClimateVariantOf() {
        return null;
    }
}
