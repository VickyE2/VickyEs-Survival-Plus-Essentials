package org.vicky.vspe.systems.Dimension.Dimensions.ChromaticUnderWater.Biomes;

import org.bukkit.block.Biome;
import org.vicky.vspe.systems.Dimension.Dimensions.ChromaticUnderWater.Palettes.MossOceanFloor;
import org.vicky.vspe.systems.Dimension.Generator.utils.Colorable;
import org.vicky.vspe.systems.Dimension.Generator.utils.NoiseSampler;
import org.vicky.vspe.systems.Dimension.Generator.utils.NoiseSamplerBuilder;
import org.vicky.vspe.systems.Dimension.Generator.utils.Rarity;
import org.vicky.vspe.systems.Dimension.Generator.utils.Biome.BaseBiome;
import org.vicky.vspe.systems.Dimension.Generator.utils.Biome.extend.Extendibles;
import org.vicky.vspe.systems.Dimension.Generator.utils.Biome.extend.Tags;
import org.vicky.vspe.systems.Dimension.Generator.utils.Biome.type.Precipitation;
import org.vicky.vspe.systems.Dimension.Generator.utils.Biome.type.subEnums.Ocean_Flat;
import org.vicky.vspe.systems.Dimension.Generator.utils.Variant.BiomeVariant;

import java.util.Map;

public class CoralReefs extends BaseBiome implements BiomeVariant {
   public CoralReefs() {
      super("CORAL_REEF", 4909768, Biome.WARM_OCEAN, Ocean_Flat.TROPICAL, new Precipitation(0.7F, Precipitation.PrecipitaionType.RAIN), Rarity.RARE);
      this.addTag(Tags.USE_UNDERWATER_RAVINE);
      this.addColor(Colorable.WATER, Integer.valueOf(6158037));
      this.addColor(Colorable.WATER_FOG, Integer.valueOf(93781794));
      this.addExtendible(Extendibles.ENVIRONMENT_MARINE_OCEAN);
      this.addExtendible(Extendibles.EQ_FLAT);
      this.addPalettes(new MossOceanFloor(), 319);

      NoiseSampler warpX = NoiseSamplerBuilder.of(NoiseSampler.DOMAIN_WARP)
              .addGlobalParameter("dimensions", 2)
              .setParameter("sampler", NoiseSamplerBuilder
                      .of(NoiseSampler.OPEN_SIMPLEX_2S)
                      .setParameter("frequency", 0.007)
                      .build()
              )
              .setParameter("warp", NoiseSamplerBuilder
                      .of(NoiseSampler.OPEN_SIMPLEX_2)
                      .setParameter("frequency", 0.0008)
                      .build()
              )
              .setParameter("amplitude", 50)
              .build();

      NoiseSampler warpZ = NoiseSamplerBuilder.of(NoiseSampler.DOMAIN_WARP)
              .addGlobalParameter("dimensions", 2)
              .setParameter("sampler", NoiseSamplerBuilder
                      .of(NoiseSampler.OPEN_SIMPLEX_2S)
                      .setParameter("frequency", 0.007)
                      .build()
              )
              .setParameter("warp", NoiseSamplerBuilder
                      .of(NoiseSampler.OPEN_SIMPLEX_2)
                      .setParameter("frequency", 0.0008)
                      .build()
              )
              .setParameter("amplitude", 50)
              .build();

      NoiseSampler terrain = NoiseSamplerBuilder.of(NoiseSampler.LINEAR_HEIGHTMAP)
              .setParameter("base", 64)
              .setParameter("sampler",
                      NoiseSamplerBuilder
                              .of(NoiseSampler.EXPRESSION)
                              .setParameter("expression", "base(x, z) + simplex2d(x * 0.02 + warpX(x, 1), z * 0.02 + warpZ(1, z)) * 10")
                              .setParameter("samplers", Map.of(
                                      "base", NoiseSamplerBuilder.of(NoiseSampler.CONSTANT)
                                              .addGlobalParameter("dimensions", 2)
                                              .setParameter("value", 64)
                                              .build(),
                                      "simplex2d", NoiseSamplerBuilder.of(NoiseSampler.OPEN_SIMPLEX_2)
                                              .addGlobalParameter("dimensions", 2)
                                              .setParameter("frequency", 0.02)
                                              .build(),
                                      "warpX", warpX,
                                      "warpZ", warpZ
                              ))
                              .build()
              )
              .build();
      setTerrain(terrain);
   }

   @Override
   public String getVariantName() {
      return "CORAL_REEFS";
   }

   @Override
   public Rarity getSelfRarity() {
      return Rarity.COMMON;
   }
}
