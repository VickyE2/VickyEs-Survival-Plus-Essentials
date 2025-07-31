package org.vicky.vspe.systems.dimension.dimensions.ChromaticUnderWater.Biomes;

import org.bukkit.block.Biome;
import org.vicky.vspe.systems.dimension.dimensions.ChromaticUnderWater.Palettes.SandyOceanFloor;
import org.vicky.vspe.systems.dimension.Generator.utils.Biome.BaseBiome;
import org.vicky.vspe.systems.dimension.Generator.utils.Biome.extend.Extendibles;
import org.vicky.vspe.systems.dimension.Generator.utils.Biome.extend.Tags;
import org.vicky.vspe.systems.dimension.Generator.utils.Biome.type.Precipitation;
import org.vicky.vspe.systems.dimension.Generator.utils.Biome.type.subEnums.Ocean_Flat;
import org.vicky.vspe.systems.dimension.Generator.utils.Colorable;
import org.vicky.vspe.systems.dimension.Generator.utils.Rarity;
import org.vicky.vspe.systems.dimension.Generator.utils.Variant.BiomeVariant;
import org.vicky.vspe.systems.dimension.Generator.utils.Variant.ClimateVariant;
import org.vicky.vspe.systems.dimension.Generator.utils.Variant.Variant;

public class DeadSeagrassMeadows extends BaseBiome implements Variant {
    public DeadSeagrassMeadows() {
        super(
                "DEAD_SEAGRASS_MEADOWS",
                0x334455,
                Biome.WARM_OCEAN,
                Ocean_Flat.TEMPERATE,
                new Precipitation(6, Precipitation.PrecipitaionType.SNOW),
                Rarity.COMMON
        );
        this.addTag(Tags.USE_UNDERWATER_RAVINE);
        this.addColor(Colorable.WATER, 0xb0ad60);
        this.addColor(Colorable.WATER_FOG, 0xb0ad60);
        this.addColor(Colorable.GRASS, 0xb0ad60);
        this.addColor(Colorable.FOLIAGE, 0xb0ad60);
        this.addPalettes(new SandyOceanFloor(), 319);
        addExtendible(Extendibles.EQ_GLOBAL_OCEAN);
        isOcean();
    }

    @Override
    public Rarity getVariantRarity() {
        return Rarity.LEGENDARY;
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
