package org.vicky.vspe.systems.dimension.dimensions.ChromaticUnderWater.Palettes;

import org.bukkit.Material;
import org.vicky.vspe.systems.dimension.Generator.utils.NoiseSampler.NoiseSampler;
import org.vicky.vspe.systems.dimension.Generator.utils.NoiseSampler.Samplers.CONSTANT;
import org.vicky.vspe.systems.dimension.Generator.utils.NoiseSampler.Samplers.EXPRESSION;
import org.vicky.vspe.systems.dimension.Generator.utils.NoiseSampler.Samplers.FBM;
import org.vicky.vspe.systems.dimension.Generator.utils.NoiseSampler.Samplers.OPEN_SIMPLEX_2;
import org.vicky.vspe.systems.dimension.Generator.utils.Palette.Palette;

import java.util.Map;

public class MossOceanFloor extends Palette {
    public MossOceanFloor() {
        super("MOSS_FLOOR_PALETTE");
        NoiseSampler openSimplex = new OPEN_SIMPLEX_2().setParameter("frequency", 0.001);
        addLayer(
                Map.of(
                        Material.MOSS_BLOCK, 6,
                        Material.SAND, 1
                ),
                1,
                new EXPRESSION()
                        .setParameter("expression", "moss_mask(x, z) * (1 - ((distance(x, z) - edge_distance(x, z)) / transition_distance(x, z))) + sand_mask(x, z) * ((distance(x, z) - edge_distance(x, z)) / transition_distance(x, z))")
                        .addGlobalParameter("dimensions", 2)
                        .setParameter("samplers", Map.of(
                                "moss_mask", new FBM()
                                        .addGlobalParameter("dimensions", 2)
                                        .setParameter("octaves", 4)
                                        .setParameter("sampler", openSimplex),
                                "sand_mask", new FBM()
                                        .addGlobalParameter("dimensions", 2)
                                        .setParameter("octaves", 4)
                                        .setParameter("sampler", openSimplex),
                                "distance", new EXPRESSION()
                                        .addGlobalParameter("dimensions", 2)
                                        .setParameter("expression", "sqrt((x^2) +(z^2))"),
                                "edge_distance", new CONSTANT()
                                        .addGlobalParameter("dimensions", 2)
                                        .setParameter("value", 5),
                                "transition_distance", new CONSTANT()
                                        .addGlobalParameter("dimensions", 2)
                                        .setParameter("value", 5))
                        ).getYml()
        );
    }
}
