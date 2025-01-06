package org.vicky.vspe.systems.Dimension.Dimensions.Test;

import org.vicky.vspe.systems.Dimension.Dimensions.Test.Biomes.*;
import org.vicky.vspe.systems.Dimension.Generator.BaseGenerator;
import org.vicky.vspe.systems.Dimension.Generator.utils.Biome.extend.Tags;
import org.vicky.vspe.systems.Dimension.Generator.utils.Extrusion.ReplaceExtrusion;
import org.vicky.vspe.systems.Dimension.Generator.utils.GlobalPreprocessor;
import org.vicky.vspe.systems.Dimension.Generator.utils.Meta.Base;
import org.vicky.vspe.systems.Dimension.Generator.utils.Meta.MetaClass;
import org.vicky.vspe.systems.Dimension.Generator.utils.Range;
import org.vicky.vspe.systems.Dimension.Generator.utils.Structures.NoiseSampler.NoiseSampler;
import org.vicky.vspe.systems.Dimension.Generator.utils.Utilities;

public class TestGenerator extends BaseGenerator {

    public TestGenerator() {
        super("test_generator", "1.0.0-TEST", "VickyE2");
        addBiome(new TestBiome());
        addBiome(new TestBiome2());
        addBiome(new TestBiome3());
        addBiome(new TestBiome4());
        MetaClass metaClass = new MetaClass();
        Base base = new Base();

        metaClass.oceanLevel = 80;
        metaClass.setHeightVariance(3000);
        metaClass.setTemperatureVariance(-0.3);

        base.addPreprocessor(GlobalPreprocessor.UNDERGROUND_LAVA_COLUMNS);

        META = metaClass;
        BASE = base;

        ReplaceExtrusion extrusion = new ReplaceExtrusion("add_underground_biome", 2);
        extrusion.setFrom(Tags.ALL);
        extrusion.setRange(new Range(-20, 10));
        NoiseSampler sampler = NoiseSampler.CELLULAR;
            sampler.setParameter("return", "CellValue");
            sampler.setParameter("salt", Utilities.generateRandomNumber());
            sampler.setParameter("frequency", "1 / 200 / ${customization.yml:cave-biome-scale} / ${customization.yml:global-scale}");
        extrusion.addBiome(new TestExtrusionBiome(), 4);
    }
}
