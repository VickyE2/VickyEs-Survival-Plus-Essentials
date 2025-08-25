package org.vicky.vspe.platform.systems.dimension.globalDimensions;

import kotlin.Pair;
import org.jetbrains.annotations.NotNull;
import org.vicky.platform.PlatformPlugin;
import org.vicky.platform.utils.ResourceLocation;
import org.vicky.vspe.BiomeCategory;
import org.vicky.vspe.PrecipitationType;
import org.vicky.vspe.platform.VSPEPlatformPlugin;
import org.vicky.vspe.platform.systems.dimension.vspeChunkGenerator.*;

import java.util.List;

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
                        .add(new Pair<>(0.01, 0.07), VSPEPlatformPlugin.<B>biomeFactory().createBiome(BiomeDetailHolder.MAGENTA_FOREST))
                        .add(new Pair<>(0.07, 0.4), VSPEPlatformPlugin.<B>biomeFactory().createBiome(BiomeDetailHolder.FRIGID_SEA))
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
                0.6,
                0.5,
                BiomeCategory.PLAINS,
                false,
                true,
                false,
                PrecipitationType.SNOW,
                /*BiomeBlockDistributionPalette.Companion.empty()*/
                new BiomeBlockDistributionPaletteBuilder<>()
                        .addLayer(319, -64,
                                PlatformPlugin.stateFactory().getBlockState("crymorra:magenta_grass_block")
                        )
                        .addShoreLayer(3, 5,
                                PlatformPlugin.stateFactory().getBlockState("vspe:pink_sand")
                        )
                        .addUnderwaterLayer(0, 40,
                                PlatformPlugin.stateFactory().getBlockState("minecraft:mud")
                        )
                        .build(),
                List.of(
                        new NoiseLayer(
                                new TerrainSamplerBuilder(544965145)
                                        .setUseRidgedMountains(true)
                                        .setMountaininess(0.025)
                                        .setMountainFrequency(0.0005)
                                        .setMountainRarity(0.12)
                                        .setHillFrequency(0.0004)
                                        .setHilliness(0.037)
                                        .setGentleWeight(0.7)
                                        .setGentleFrequency(0.0067)
                                        .setBaseHeight(64)
                                        .setMaxHeight(164)
                                        .buildHeightMapper(true),
                                1,
                                NoiseLayer.Mode.HEIGHT
                        )
                ),
                List.of(),
                new BiomeStructureData(List.of(
                        ResourceLocation.from("crymorra:pink_frost_tree")
                ))
        );
        public static final BiomeParameters FRIGID_SEA = new BiomeParameters(
                "crymorra:frigid_sea",
                "The Frigid Sea",
                0xa6a6a6,
                0x707070,
                0x515151,
                0x454545,
                0x707070,
                0xb6b6b6,
                false,
                0.4,
                1.0,
                0.0,
                0.5,
                BiomeCategory.COLD_OCEAN,
                false,
                true,
                false,
                PrecipitationType.SNOW,
                /*BiomeBlockDistributionPalette.Companion.empty()*/
                new BiomeBlockDistributionPaletteBuilder<>()
                        .addLayer(319, -64,
                                PlatformPlugin.stateFactory().getBlockState("crymorra:magenta_grass_block"),
                                1.0)
                        .build(),
                List.of(
                        new NoiseLayer(
                                new TerrainSamplerBuilder(233)
                                        .setUseRidgedMountains(false)
                                        .setMountaininess(0.00025)
                                        .setMountainFrequency(0.00005)
                                        .setMountainRarity(0.002)
                                        .setHillFrequency(0.004)
                                        .setHilliness(0.057)
                                        .setGentleWeight(0.4)
                                        .setGentleFrequency(0.0223)
                                        .setBaseHeight(11)
                                        .setMaxHeight(64)
                                        .buildHeightMapper(true),
                                1,
                                NoiseLayer.Mode.HEIGHT
                        )
                )
        );
    }
}