package org.vicky.vspe.systems.dimension.dimensions.ChromaticUnderWater.Biomes;

import org.bukkit.block.Biome;
import org.vicky.vspe.systems.dimension.dimensions.ChromaticUnderWater.Palettes.MossOceanFloor;
import org.vicky.vspe.systems.dimension.Generator.utils.Biome.BaseBiome;
import org.vicky.vspe.systems.dimension.Generator.utils.Biome.extend.Tags;
import org.vicky.vspe.systems.dimension.Generator.utils.Biome.type.Precipitation;
import org.vicky.vspe.systems.dimension.Generator.utils.Biome.type.subEnums.Ocean_Flat;
import org.vicky.vspe.systems.dimension.Generator.utils.Colorable;
import org.vicky.vspe.systems.dimension.Generator.utils.NoiseSampler.NoiseSampler;
import org.vicky.vspe.systems.dimension.Generator.utils.NoiseSampler.NoiseSamplerBuilder;
import org.vicky.vspe.systems.dimension.Generator.utils.NoiseSampler.Samplers.*;
import org.vicky.vspe.systems.dimension.Generator.utils.Rarity;
import org.vicky.vspe.systems.dimension.Generator.utils.Variant.BiomeVariant;
import org.vicky.vspe.systems.dimension.Generator.utils.Variant.ClimateVariant;
import org.vicky.vspe.systems.dimension.Generator.utils.Variant.Variant;

import java.util.Map;

public class MysticalCoralReef extends BaseBiome implements Variant {
    public MysticalCoralReef() {
        super(
                "MYSTICAL_CORAL_REEF", 0xFA9BFD, Biome.WARM_OCEAN, Ocean_Flat.TROPICAL, new Precipitation(0.7F, Precipitation.PrecipitaionType.RAIN), Rarity.LEGENDARY
        );
        this.addTag(Tags.USE_UNDERWATER_RAVINE);
        this.addColor(Colorable.WATER, 0xde78f8);
        this.addColor(Colorable.WATER_FOG, 0xde78f8);
        this.addExtendible(new CoralReefs());
        this.addPalettes(new MossOceanFloor(), 319);

        NoiseSampler warpX = NoiseSamplerBuilder.of(new DOMAIN_WARP())
                .addGlobalParameter("dimensions", 2)
                .setParameter("sampler", NoiseSamplerBuilder
                        .of(new OPEN_SIMPLEX_2S())
                        .setParameter("frequency", 0.01) // Slightly higher frequency for more noticeable warp
                        .build()
                )
                .setParameter("warp", NoiseSamplerBuilder
                        .of(new OPEN_SIMPLEX_2S())
                        .setParameter("frequency", 0.002) // Increased frequency for finer distortions
                        .build()
                )
                .setParameter("amplitude", 8) // Increased warp amplitude
                .build();

        NoiseSampler warpZ = NoiseSamplerBuilder.of(new DOMAIN_WARP())
                .addGlobalParameter("dimensions", 2)
                .setParameter("sampler", NoiseSamplerBuilder
                        .of(new OPEN_SIMPLEX_2S())
                        .setParameter("frequency", 0.01)
                        .build()
                )
                .setParameter("warp", NoiseSamplerBuilder
                        .of(new OPEN_SIMPLEX_2S())
                        .setParameter("frequency", 0.002)
                        .build()
                )
                .setParameter("amplitude", 8) // Match warp amplitude for consistency
                .build();

        NoiseSampler terrain = NoiseSamplerBuilder.of(new LINEAR_HEIGHTMAP())
                .setParameter("base", 30) // Keep base height constant
                .setParameter("sampler",
                        NoiseSamplerBuilder
                                .of(new EXPRESSION())
                                .setParameter("expression", "base(x, z) + simplex2d(x * 0.05 + warpX(z, 0.5), z * 0.05 + warpZ(0.5, x)) * 20")
                                .setParameter("samplers", Map.of(
                                        "base", NoiseSamplerBuilder.of(new CONSTANT())
                                                .addGlobalParameter("dimensions", 2)
                                                .setParameter("value", 64) // Base height value
                                                .build(),
                                        "simplex2d", NoiseSamplerBuilder.of(new OPEN_SIMPLEX_2S())
                                                .addGlobalParameter("dimensions", 2)
                                                .setParameter("frequency", 0.1) // Higher frequency for steeper terrain
                                                .build(),
                                        "warpX", warpX,
                                        "warpZ", warpZ
                                ))
                                .build()
                )
                .build();

        setTerrain(terrain.getYml());
    }


    @Override
    public Rarity getVariantRarity() {
        return Rarity.COMMON;
    }

    @Override
    public ClimateVariant getClimateVariantOf() {
        return null;
    }

    @Override
    public BiomeVariant getVariantOf() {
        return new CoralReefs();
    }


}
