package org.vicky.vspe.platform.systems.dimension.terrasupporteddimensions.dimensions.ChromaticUnderWater.Palettes;

import org.vicky.vspe.platform.systems.dimension.terrasupporteddimensions.Generator.utils.NoiseSampler.Samplers.CONSTANT;
import org.vicky.vspe.platform.systems.dimension.terrasupporteddimensions.Generator.utils.Palette.Palette;
import org.vicky.vspe.platform.systems.dimension.vspeChunkGenerator.SimpleBlockState;

import java.util.Map;

public class SandyOceanFloor extends Palette {
    public SandyOceanFloor() {
        super("SANDY_FLOOR_PALETTE");
        addLayer(
                Map.of(
                        SimpleBlockState.Companion.from("minecraft:sand", (it) -> it).getMaterial(), 6
                ),
                1,
                new CONSTANT()
                        .getYml()
        );
    }
}
