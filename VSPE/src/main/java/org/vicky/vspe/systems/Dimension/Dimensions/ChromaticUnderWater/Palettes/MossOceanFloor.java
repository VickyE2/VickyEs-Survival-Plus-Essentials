package org.vicky.vspe.systems.Dimension.Dimensions.ChromaticUnderWater.Palettes;

import java.util.HashMap;
import java.util.Map;
import org.bukkit.Material;
import org.vicky.vspe.systems.Dimension.Generator.utils.DimensionedSampler;
import org.vicky.vspe.systems.Dimension.Generator.utils.NoiseSampler;
import org.vicky.vspe.systems.Dimension.Generator.utils.NoiseSamplerBuilder;
import org.vicky.vspe.systems.Dimension.Generator.utils.Palette.LayerClass;
import org.vicky.vspe.systems.Dimension.Generator.utils.Palette.Palette;

public class MossOceanFloor extends Palette {
   public MossOceanFloor() {
      super("MOSS_FLOOR_PALETTE");
      LayerClass moss_to_sand = new LayerClass(4);
      moss_to_sand.addMaterial(Material.SAND, 1);
      moss_to_sand.addMaterial(Material.MOSS_BLOCK, 6);
      NoiseSampler sampler = NoiseSampler.EXPRESSION;
      Map<String, Object> samplers = new HashMap<>();
      samplers.put(
         "moss_mask",
         new DimensionedSampler(
            2,
            NoiseSamplerBuilder.of(NoiseSampler.FBM)
               .setParameter("octaves", 4)
               .setParameter("sampler", NoiseSamplerBuilder.of(NoiseSampler.OPEN_SIMPLEX_2).setParameter("frequency", 0.01).build())
               .build()
         )
      );
      samplers.put(
         "sand_mask",
         new DimensionedSampler(
            2,
            NoiseSamplerBuilder.of(NoiseSampler.FBM)
               .setParameter("octaves", 4)
               .setParameter("sampler", NoiseSamplerBuilder.of(NoiseSampler.OPEN_SIMPLEX_2).setParameter("frequency", 0.01).build())
               .build()
         )
      );
      samplers.put("distance", new DimensionedSampler(2, NoiseSamplerBuilder.of(NoiseSampler.EXPRESSION).setParameter("expression", "sqrt(x^2 + z^2)").build()));
      samplers.put("edge_distance", new DimensionedSampler(1, NoiseSamplerBuilder.of(NoiseSampler.CONSTANT).setParameter("value", 5).build()));
      samplers.put("transition_distance", new DimensionedSampler(1, NoiseSamplerBuilder.of(NoiseSampler.CONSTANT).setParameter("value", 5).build()));
      sampler.setParameter("samplers", samplers);
      sampler.setParameter(
         "expression", "|\nblend = clamp((distance(x, z) - edge_distance) / transition_distance, 0, 1);\nlerp(moss_mask(x, z), sand_mask(x, z), blend)\n"
      );
      moss_to_sand.setSampler(sampler);
      this.addLayer(Map.of(Material.MOSS_BLOCK, 1), 3);
      this.addLayer(Map.of(Material.MOSS_BLOCK, 2, Material.SAND, 1), 2);
      this.addLayer(Map.of(Material.SAND, 1, Material.STONE, 2), 3);
   }
}
