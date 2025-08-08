package org.vicky.vspe.platform.systems.dimension.terrasupporteddimensions.dimensions.Test;

import org.vicky.utilities.Version;
import org.vicky.vspe.platform.systems.dimension.terrasupporteddimensions.dimensions.Test.Biomes.*;
import org.vicky.vspe.platform.systems.dimension.terrasupporteddimensions.dimensions.Test.Biomes.*;
import org.vicky.vspe.platform.systems.dimension.terrasupporteddimensions.dimensions.Test.Biomes.*;
import org.vicky.vspe.platform.systems.dimension.terrasupporteddimensions.Generator.BaseGenerator;
import org.vicky.vspe.platform.systems.dimension.terrasupporteddimensions.Generator.utils.Biome.extend.Tags;
import org.vicky.vspe.platform.systems.dimension.terrasupporteddimensions.Generator.utils.Extrusion.ReplaceExtrusion;
import org.vicky.vspe.platform.systems.dimension.terrasupporteddimensions.Generator.utils.GlobalPreprocessor;
import org.vicky.vspe.platform.systems.dimension.terrasupporteddimensions.Generator.utils.Meta.BaseClass;
import org.vicky.vspe.platform.systems.dimension.terrasupporteddimensions.Generator.utils.NoiseSampler.Samplers.CELLULAR;
import org.vicky.vspe.platform.systems.dimension.terrasupporteddimensions.Generator.utils.Range;
import org.vicky.vspe.platform.systems.dimension.terrasupporteddimensions.Generator.utils.Utilities;

public class TestGenerator extends BaseGenerator {
    public TestGenerator() {
        super("test_generator", Version.parse("1.0.0-TEST"), "VickyE2");
        this.addBiome(new TestBiome());
        this.addBiome(new TestBiome2());
        this.addBiome(new TestBiome3());
        this.addBiome(new TestBiome4());
        BaseClass baseClass = new BaseClass();
        baseClass.addPreprocessor(GlobalPreprocessor.UNDERGROUND_LAVA_COLUMNS);
        this.META = new TestMeta();
        this.BASEClass = baseClass;
        ReplaceExtrusion extrusion = new ReplaceExtrusion("add_underground_biome", 2);
        extrusion.setFrom(Tags.ALL);
        extrusion.setRange(new Range(-20, 10));
        CELLULAR sampler = new CELLULAR();
        sampler.setParameter("return", "CellValue");
        sampler.setParameter("salt", Utilities.generateRandomNumber());
        sampler.setParameter("frequency", "1 / 200 / ${customization.yml:cave-biome-scale} / ${customization.yml:global-scale}");
        extrusion.addBiome(new TestExtrusionBiome(), 4);
        extrusion.setSampler(sampler);
        this.addExtrusion(extrusion);
    }
}
