package org.vicky.vspe.systems.Dimension.Dimensions.Test;

import org.vicky.vspe.systems.Dimension.Dimensions.Test.Biomes.TestBiome;
import org.vicky.vspe.systems.Dimension.Dimensions.Test.Biomes.TestBiome2;
import org.vicky.vspe.systems.Dimension.Dimensions.Test.Biomes.TestBiome3;
import org.vicky.vspe.systems.Dimension.Dimensions.Test.Biomes.TestBiome4;
import org.vicky.vspe.systems.Dimension.Generator.BaseGenerator;
import org.vicky.vspe.systems.Dimension.Generator.utils.GlobalPreprocessor;
import org.vicky.vspe.systems.Dimension.Generator.utils.Meta.Base;
import org.vicky.vspe.systems.Dimension.Generator.utils.Meta.MetaClass;

public class TestGenerator extends BaseGenerator {

    public TestGenerator() {
        super("test_generator", "1.0.0-TEST", "VickyE2");
        addBiome(new TestBiome());
        addBiome(new TestBiome2());
        addBiome(new TestBiome3());
        addBiome(new TestBiome4());
        MetaClass metaClass = new MetaClass();
        Base base = new Base();

        metaClass.oceanLevel = 135;
        metaClass.setHeightVariance(3000);
        metaClass.setTemperatureVariance(-0.3);

        base.addPreprocessor(GlobalPreprocessor.UNDERGROUND_LAVA_COLUMNS);

        META = metaClass;
        BASE = base;
    }
}
