package org.vicky.vspe.platform.systems.dimension.globalDimensions.Crymorra;

import kotlin.Pair;
import org.jetbrains.annotations.NotNull;
import org.vicky.platform.utils.ResourceLocation;
import org.vicky.platform.world.PlatformBlockState;
import org.vicky.vspe.BiomeCategory;
import org.vicky.vspe.PrecipitationType;
import org.vicky.vspe.platform.VSPEPlatformPlugin;
import org.vicky.vspe.platform.systems.dimension.vspeChunkGenerator.*;

public abstract class CrymorraDimension<T, B extends PlatformBiome> implements PlatformDimension<T, B> {
    @Override
    public @NotNull BiomeResolver<@NotNull B> getBiomeResolver() {
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
    

    private static class BiomeDetailHolder {
        static final BiomeParameters MAGENTA_FOREST = new BiomeParameters(
                "crymorra:magenta_forest",
                "Magenta Forest",
                0xff3761,
                0xff30a7,
                0x00CCFF,
                0xAACCFF,
                false,
                0.4,
                0.7,
                0.3,
                0.5,
                BiomeCategory.SNOWY_BEACH,
                PrecipitationType.SNOW,
                new BiomeBlockDistributionPaletteBuilder<>()
                        .addLayer(319, -64, VSPEPlatformPlugin.blockStateCreator().getBlockState(ResourceLocation.from("crymorra:magenta_grass_block")))
                        .build()
        );
    }
}