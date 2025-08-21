package org.vicky.vspe.platform.systems.dimension.globalDimensions;

import kotlin.Pair;
import org.jetbrains.annotations.NotNull;
import org.vicky.vspe.BiomeCategory;
import org.vicky.vspe.PrecipitationType;
import org.vicky.vspe.platform.VSPEPlatformPlugin;
import org.vicky.vspe.platform.systems.dimension.vspeChunkGenerator.*;

public final class BiomeResolvers<B extends PlatformBiome> {
    private static BiomeResolvers<?> INSTANCE;

    private BiomeResolvers() {
    }

    @SuppressWarnings("unchecked")
    public static <B extends PlatformBiome> BiomeResolvers<B> getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new BiomeResolvers<>();
        }
        return (BiomeResolvers<B>) INSTANCE;
    }

    public BiomeResolver<@NotNull B> CRYMORRA_BIOME_RESOLVER() {
        return new MultiParameterBiomeResolver<>(
                new JNoiseNoiseSampler(
                        NoiseSamplerFactory.INSTANCE.create(
                                NoiseSamplerFactory.Type.PERLIN,
                                b -> {
                                    b.addModule(
                                            AdditionModuleBuilder.Companion.newBuilder()
                                                    .withSecondary(
                                                            NoiseSamplerFactory.INSTANCE.create(
                                                                    NoiseSamplerFactory.Type.PERLIN,
                                                                    b2 -> b2, // same as `{ it }` in Kotlin
                                                                    0L,
                                                                    0.03,
                                                                    2,
                                                                    2.0
                                                            )
                                                    )
                                                    .build()
                                    );
                                    return b;
                                },
                                0L,
                                0.005,
                                4,
                                3.0
                        )
                ),
                new JNoiseNoiseSampler(
                        NoiseSamplerFactory.INSTANCE.create(
                                NoiseSamplerFactory.Type.PERLIN,
                                b -> {
                                    b.addModule(
                                            AdditionModuleBuilder.Companion.newBuilder()
                                                    .withSecondary(
                                                            NoiseSamplerFactory.INSTANCE.create(
                                                                    NoiseSamplerFactory.Type.PERLIN,
                                                                    b2 -> b2,
                                                                    0L,
                                                                    0.07,
                                                                    2,
                                                                    2.0
                                                            )
                                                    )
                                                    .build()
                                    );
                                    return b;
                                },
                                0L,
                                0.01,
                                2,
                                5.0
                        )
                ),
                new JNoiseNoiseSampler(
                        NoiseSamplerFactory.INSTANCE.create(
                                NoiseSamplerFactory.Type.PERLIN,
                                b -> {
                                    b.addModule(
                                            AdditionModuleBuilder.Companion.newBuilder()
                                                    .withSecondary(
                                                            NoiseSamplerFactory.INSTANCE.create(
                                                                    NoiseSamplerFactory.Type.PERLIN,
                                                                    b2 -> b2,
                                                                    0L,
                                                                    0.3,
                                                                    2,
                                                                    2.0
                                                            )
                                                    )
                                                    .build()
                                    );
                                    return b;
                                },
                                0L,
                                0.001,
                                4,
                                3.4
                        )
                ), new JNoiseNoiseSampler(
                NoiseSamplerFactory.INSTANCE.create(
                        NoiseSamplerFactory.Type.PERLIN,
                        b -> {
                            b.addModule(
                                    AdditionModuleBuilder.Companion.newBuilder()
                                            .withSecondary(
                                                    NoiseSamplerFactory.INSTANCE.create(
                                                            NoiseSamplerFactory.Type.PERLIN,
                                                            b2 -> b2,
                                                            0L,
                                                            0.005,
                                                            2,
                                                            2.0
                                                    )
                                            )
                                            .build()
                            );
                            return b;
                        },
                        0L,
                        0.01,
                        2,
                        1.5
                )
        ),
                new PaletteBuilder<B>()
                        .add(new Pair<>(0.01, 0.02), VSPEPlatformPlugin.<B>biomeFactory().createBiome(BiomeDetailHolder.MAGENTA_FOREST))
                        .build()
        );
    }

    public static final class BiomeDetailHolder {
        public static final BiomeParameters MAGENTA_FOREST = new BiomeParameters(
                "crymorra:magenta_forest",
                "Magenta Forest",
                0xff3761,
                0xff30a7,
                0x00CCFF,
                0xAACCFF,
                0xff96d3,
                0xeab7ff,
                false,
                0.4,
                0.7,
                0.3,
                0.5,
                BiomeCategory.SNOWY_BEACH,
                false,
                true,
                false,
                PrecipitationType.SNOW,
                BiomeBlockDistributionPalette.Companion.empty()
                /*new BiomeBlockDistributionPaletteBuilder<>()
                        .addLayer(319, -64, VSPEPlatformPlugin.blockStateCreator().getBlockState(ResourceLocation.from("crymorra:magenta_grass_block")))
                        .build()*/,
                new TerrainSamplerBuilder(544965145)
                        .setUseRidgedMountains(false)
                        .setMountaininess(0.025)
                        .setMountainFrequency(0.0005)
                        .setMountainRarity(0.2)
                        .setHillFrequency(0.004)
                        .setHilliness(0.037)
                        .setGentleWeight(0.7)
                        .setBaseHeight(64)
                        .setMaxHeight(164)
                        .buildSampler()
        );
    }
}