package org.vicky.vspe.systems.Dimension.Dimensions.Test;

import org.vicky.vspe.systems.Dimension.Dimensions.Test.Biomes.TestBiome;
import org.vicky.vspe.systems.Dimension.Generator.BaseGenerator;
import org.vicky.vspe.systems.Dimension.Generator.utils.GlobalPreprocessor;
import org.vicky.vspe.systems.Dimension.Generator.utils.Meta.Base;
import org.vicky.vspe.systems.Dimension.Generator.utils.Meta.MetaClass;

public class TestGenerator extends BaseGenerator {

    public TestGenerator() {
        super("test_generator", "1.0.0-TEST", "VickyE2");
        addBiome(new TestBiome());
        MetaClass metaClass = new MetaClass();
        Base base = new Base();

        metaClass.oceanLevel = 319;
        base.addPreprocessor(GlobalPreprocessor.TEXTURED_STONE_SLANT);
        base.addPreprocessor(GlobalPreprocessor.UNDERGROUND_LAVA_COLUMNS);

        META = metaClass;
        BASE = base;
    }
}
