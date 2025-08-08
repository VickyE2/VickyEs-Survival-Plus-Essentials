package org.vicky.vspe.platform.systems.dimension.terrasupporteddimensions.dimensions.ChromaticUnderWater.Biomes;

import org.vicky.vspe.BiomeCategory;
import org.vicky.vspe.platform.systems.dimension.terrasupporteddimensions.Generator.utils.NoiseSampler.Samplers.EXPRESSION;
import org.vicky.vspe.platform.systems.dimension.terrasupporteddimensions.Generator.utils.NoiseSampler.Samplers.LINEAR_HEIGHTMAP;
import org.vicky.vspe.platform.systems.dimension.terrasupporteddimensions.Generator.utils.NoiseSampler.Samplers.OPEN_SIMPLEX_2;
import org.vicky.vspe.platform.systems.dimension.terrasupporteddimensions.dimensions.ChromaticUnderWater.Features.SeagrassFloor;
import org.vicky.vspe.platform.systems.dimension.terrasupporteddimensions.dimensions.ChromaticUnderWater.Palettes.SandyOceanFloor;
import org.vicky.vspe.platform.systems.dimension.terrasupporteddimensions.Generator.utils.Biome.BaseBiome;
import org.vicky.vspe.platform.systems.dimension.terrasupporteddimensions.Generator.utils.Biome.extend.Extendibles;
import org.vicky.vspe.platform.systems.dimension.terrasupporteddimensions.Generator.utils.Biome.extend.Tags;
import org.vicky.vspe.platform.systems.dimension.terrasupporteddimensions.Generator.utils.Biome.type.Precipitation;
import org.vicky.vspe.platform.systems.dimension.terrasupporteddimensions.Generator.utils.Biome.type.subEnums.Ocean_Flat;
import org.vicky.vspe.platform.systems.dimension.terrasupporteddimensions.Generator.utils.Colorable;
import org.vicky.vspe.platform.systems.dimension.terrasupporteddimensions.Generator.utils.Feature.Featureable;
import org.vicky.vspe.platform.systems.dimension.terrasupporteddimensions.Generator.utils.NoiseSampler.Function;
import org.vicky.vspe.platform.systems.dimension.terrasupporteddimensions.Generator.utils.NoiseSampler.NoiseSampler;
import org.vicky.vspe.platform.systems.dimension.terrasupporteddimensions.Generator.utils.NoiseSampler.NoiseSamplerBuilder;
import org.vicky.vspe.platform.systems.dimension.terrasupporteddimensions.Generator.utils.NoiseSampler.Samplers.*;
import org.vicky.vspe.platform.systems.dimension.terrasupporteddimensions.Generator.utils.Rarity;
import org.vicky.vspe.platform.systems.dimension.terrasupporteddimensions.Generator.utils.Variant.BiomeVariant;

import java.util.List;
import java.util.Map;

public class SeagrassMeadows extends BaseBiome implements BiomeVariant {
    public SeagrassMeadows() {
        super(
                "SEAGRASS_MEADOWS",
                0x334455,
                BiomeCategory.WARM_OCEAN,
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

        NoiseSampler terrain = NoiseSamplerBuilder.of(new LINEAR_HEIGHTMAP())
                .setParameter("base", 60)
                .setParameter("sampler",
                        NoiseSamplerBuilder
                                .of(new EXPRESSION())

                                .addVariable("peakHeight", 120.0)
                                .addVariable("baseRadius", 220.0)
                                .addVariable("plateauPct", 0.6)
                                .addVariable("slopePower", 2.3)
                                .addVariable("noiseAmp", 1.5)
                                .addVariable("freq", 0.007)

                                .addFunctions(
                                        new Function("clamp", "min(max(v, minV), maxV)", "v", "minV", "maxV")
                                )

                                .setParameter("expression", "max(0, peakHeight * pow(3, -slopePower * clamp((sqrt(x*x + z*z) - baseRadius * plateauPct) / (baseRadius * (1 - plateauPct)), 0, 1)) + simplex(x * freq, z * freq) * noiseAmp)")
                                .setParameter("samplers", Map.of(
                                        "simplex", NoiseSamplerBuilder.of(new OPEN_SIMPLEX_2())
                                                .addGlobalParameter("dimensions", 2)
                                                .setParameter("frequency", 0.006)
                                                .build()
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
