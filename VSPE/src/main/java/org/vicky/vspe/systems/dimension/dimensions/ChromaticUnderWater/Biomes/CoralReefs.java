package org.vicky.vspe.systems.dimension.dimensions.ChromaticUnderWater.Biomes;

import org.bukkit.block.Biome;
import org.vicky.vspe.systems.dimension.dimensions.ChromaticUnderWater.Features.LargeCorals;
import org.vicky.vspe.systems.dimension.dimensions.ChromaticUnderWater.Features.SeagrassFloor;
import org.vicky.vspe.systems.dimension.dimensions.ChromaticUnderWater.Palettes.MossOceanFloor;
import org.vicky.vspe.systems.dimension.Generator.utils.Biome.BaseBiome;
import org.vicky.vspe.systems.dimension.Generator.utils.Biome.extend.Extendibles;
import org.vicky.vspe.systems.dimension.Generator.utils.Biome.extend.Tags;
import org.vicky.vspe.systems.dimension.Generator.utils.Biome.type.Precipitation;
import org.vicky.vspe.systems.dimension.Generator.utils.Biome.type.subEnums.Ocean_Flat;
import org.vicky.vspe.systems.dimension.Generator.utils.Colorable;
import org.vicky.vspe.systems.dimension.Generator.utils.Feature.Featureable;
import org.vicky.vspe.systems.dimension.Generator.utils.NoiseSampler.NoiseSampler;
import org.vicky.vspe.systems.dimension.Generator.utils.NoiseSampler.NoiseSamplerBuilder;
import org.vicky.vspe.systems.dimension.Generator.utils.NoiseSampler.Samplers.*;
import org.vicky.vspe.systems.dimension.Generator.utils.Rarity;
import org.vicky.vspe.systems.dimension.Generator.utils.Variant.BiomeVariant;

import java.util.List;
import java.util.Map;

public class CoralReefs extends BaseBiome implements BiomeVariant {
    public CoralReefs() {
        super("CORAL_REEF", 0x490976, Biome.WARM_OCEAN, Ocean_Flat.TROPICAL, new Precipitation(0.7F, Precipitation.PrecipitaionType.RAIN), Rarity.VERY_COMMON);
        this.addTag(Tags.USE_UNDERWATER_RAVINE);
        this.addColor(Colorable.WATER, 0x3DD5A6);
        this.addColor(Colorable.WATER_FOG, 0xd5c3c6);
        this.addPalettes(new MossOceanFloor(), 319);

        addFeaturesToParam(List.of(new SeagrassFloor()), Featureable.FLORA);
        addFeaturesToParam(List.of(new LargeCorals()), Featureable.TREES);

        addExtendible(Extendibles.EQ_GLOBAL_OCEAN);
        isOcean();

        NoiseSampler warpX = NoiseSamplerBuilder.of(new DOMAIN_WARP())
                .addGlobalParameter("dimensions", 2)
                .setParameter("sampler", NoiseSamplerBuilder
                        .of(new OPEN_SIMPLEX_2S())
                        .setParameter("frequency", 0.008)
                        .build()
                )
                .setParameter("warp", NoiseSamplerBuilder
                        .of(new OPEN_SIMPLEX_2())
                        .setParameter("frequency", 0.001)
                        .build()
                )
                .setParameter("amplitude", 5)
                .build();

        NoiseSampler warpZ = NoiseSamplerBuilder.of(new DOMAIN_WARP())
                .addGlobalParameter("dimensions", 2)
                .setParameter("sampler", NoiseSamplerBuilder
                        .of(new OPEN_SIMPLEX_2())
                        .setParameter("frequency", 0.008)
                        .build()
                )
                .setParameter("warp", NoiseSamplerBuilder
                        .of(new OPEN_SIMPLEX_2())
                        .setParameter("frequency", 0.001)
                        .build()
                )
                .setParameter("amplitude", 5)
                .build();

        NoiseSampler terrain = NoiseSamplerBuilder.of(new LINEAR_HEIGHTMAP())
                .setParameter("base", 30)
                .setParameter("sampler",
                        NoiseSamplerBuilder
                                .of(new EXPRESSION())
                                .setParameter("expression", "base(x, z) + simplex2d(x * 0.02 + warpX(z, 0.5), z * 0.02 + warpZ(0.5, x)) * 10")
                                .setParameter("samplers", Map.of(
                                        "base", NoiseSamplerBuilder.of(new CONSTANT())
                                                .addGlobalParameter("dimensions", 2)
                                                .setParameter("value", 64)
                                                .build(),
                                        "simplex2d", NoiseSamplerBuilder.of(new OPEN_SIMPLEX_2S())
                                                .addGlobalParameter("dimensions", 2)
                                                .setParameter("frequency", 0.05)
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
