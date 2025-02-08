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
import org.vicky.vspe.systems.Dimension.Generator.utils.NoiseSampler.NoiseSampler;
import org.vicky.vspe.systems.Dimension.Generator.utils.NoiseSampler.NoiseSamplerBuilder;
import org.vicky.vspe.systems.Dimension.Generator.utils.NoiseSampler.Samplers.*;
import org.vicky.vspe.systems.Dimension.Generator.utils.Rarity;
import org.vicky.vspe.systems.Dimension.Generator.utils.Variant.BiomeVariant;

import java.util.List;
import java.util.Map;

public class SeagrassMeadows extends BaseBiome implements BiomeVariant {
    public SeagrassMeadows() {
        super(
                "SEAGRASS_MEADOWS",
                0x334455,
                Biome.WARM_OCEAN,
                Ocean_Flat.TEMPERATE,
                new Precipitation(6, Precipitation.PrecipitaionType.RAIN),
                Rarity.COMMON
        );
        this.addTag(Tags.USE_UNDERWATER_RAVINE);
        this.addColor(Colorable.WATER, 0x88bee0);
        this.addColor(Colorable.WATER_FOG, 0x88bee0);
        this.addPalettes(new SandyOceanFloor(), 319);
        addFeaturesToParam(List.of(new SeagrassFloor()), Featureable.UNDERWATER_FLORA);
        addExtendible(Extendibles.EQ_GLOBAL_OCEAN);
        isOcean();
        NoiseSampler warpX = NoiseSamplerBuilder.of(new DOMAIN_WARP())
                .addGlobalParameter("dimensions", 2)
                .setParameter("sampler", NoiseSamplerBuilder
                        .of(new OPEN_SIMPLEX_2S())
                        .setParameter("frequency", 0.01)
                        .build()
                )
                .setParameter("warp", NoiseSamplerBuilder
                        .of(new OPEN_SIMPLEX_2())
                        .setParameter("frequency", 0.002)
                        .build()
                )
                .setParameter("amplitude", 5)
                .build();

        NoiseSampler warpZ = NoiseSamplerBuilder.of(new DOMAIN_WARP())
                .addGlobalParameter("dimensions", 2)
                .setParameter("sampler", NoiseSamplerBuilder
                        .of(new OPEN_SIMPLEX_2())
                        .setParameter("frequency", 0.01)
                        .build()
                )
                .setParameter("warp", NoiseSamplerBuilder
                        .of(new OPEN_SIMPLEX_2())
                        .setParameter("frequency", 0.002)
                        .build()
                )
                .setParameter("amplitude", 5)
                .build();

        NoiseSampler terrain = NoiseSamplerBuilder.of(new LINEAR_HEIGHTMAP())
                .setParameter("base", 30)
                .setParameter("sampler",
                        NoiseSamplerBuilder
                                .of(new EXPRESSION())
                                .setParameter("expression", "base(x, z) + simplex2d(x * 0.002 + warpX(z, 0.2), z * 0.002 + warpZ(0.2, x)) * 5")
                                .setParameter("samplers", Map.of(
                                        "base", NoiseSamplerBuilder.of(new CONSTANT())
                                                .addGlobalParameter("dimensions", 2)
                                                .setParameter("value", 64)
                                                .build(),
                                        "simplex2d", NoiseSamplerBuilder.of(new OPEN_SIMPLEX_2S())
                                                .addGlobalParameter("dimensions", 2)
                                                .setParameter("frequency", 0.07)
                                                .build(),
                                        "warpX", warpX,
                                        "warpZ", warpZ
                                ))
                                .build()
                )
                .build();

        this.setTerrain(terrain.getYml());
    }

    @Override
    public Rarity getSelfRarity() {
        return Rarity.VERY_COMMON;
    }
}
