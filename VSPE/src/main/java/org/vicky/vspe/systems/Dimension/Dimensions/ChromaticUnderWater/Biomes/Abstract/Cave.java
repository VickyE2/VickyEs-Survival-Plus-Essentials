package org.vicky.vspe.systems.Dimension.Dimensions.ChromaticUnderWater.Biomes.Abstract;

import org.vicky.vspe.systems.Dimension.Generator.utils.Biome.BaseBiome;
import org.vicky.vspe.systems.Dimension.Generator.utils.Biome.extend.Extendibles;
import org.vicky.vspe.systems.Dimension.Generator.utils.NoiseSampler.NoiseSamplerBuilder;
import org.vicky.vspe.systems.Dimension.Generator.utils.NoiseSampler.Samplers.CONSTANT;
import org.vicky.vspe.utilities.NumberToWords;

public class Cave extends BaseBiome {
    public Cave(int size) {
        super("CHROMATIC_UNDERWATER_CAVE_" + NumberToWords.convert(size));
        addExtendible(Extendibles.CARVING_OCEAN);
        isAbstract();
        isCarving_update_palette();
        setTerrain(NoiseSamplerBuilder.of(new CONSTANT())
                .addGlobalParameter("dimensions", 3)
                .setParameter("value", size)
                .build()
                .getYml());
    }

}
