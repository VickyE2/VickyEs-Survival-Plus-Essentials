package org.vicky.vspe.systems.dimension.dimensions.ChromaticUnderWater.Palettes;

import org.bukkit.Material;
import org.vicky.vspe.systems.dimension.Generator.utils.NoiseSampler.Samplers.CONSTANT;
import org.vicky.vspe.systems.dimension.Generator.utils.Palette.Palette;

import java.util.Map;

public class SandyOceanFloor extends Palette {
    public SandyOceanFloor() {
        super("SANDY_FLOOR_PALETTE");
        addLayer(
                Map.of(
                        Material.SAND, 6
                ),
                1,
                new CONSTANT()
                        .getYml()
        );
    }
}
