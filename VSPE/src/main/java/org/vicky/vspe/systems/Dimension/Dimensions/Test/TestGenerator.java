package org.vicky.vspe.systems.Dimension.Dimensions.Test;

import org.vicky.vspe.systems.Dimension.Dimensions.Test.Biomes.TestBiome;
import org.vicky.vspe.systems.Dimension.Dimensions.Test.Biomes.TestBiome2;
import org.vicky.vspe.systems.Dimension.Dimensions.Test.Biomes.TestBiome3;
import org.vicky.vspe.systems.Dimension.Dimensions.Test.Biomes.TestBiome4;
import org.vicky.vspe.systems.Dimension.Dimensions.Test.Biomes.TestExtrusionBiome;
import org.vicky.vspe.systems.Dimension.Generator.BaseGenerator;
import org.vicky.vspe.systems.Dimension.Generator.utils.GlobalPreprocessor;
import org.vicky.vspe.systems.Dimension.Generator.utils.NoiseSampler;
import org.vicky.vspe.systems.Dimension.Generator.utils.Range;
import org.vicky.vspe.systems.Dimension.Generator.utils.Utilities;
import org.vicky.vspe.systems.Dimension.Generator.utils.Biome.extend.Tags;
import org.vicky.vspe.systems.Dimension.Generator.utils.Extrusion.ReplaceExtrusion;
import org.vicky.vspe.systems.Dimension.Generator.utils.Meta.BaseClass;

public class TestGenerator extends BaseGenerator {
   public TestGenerator() {
      super("test_generator", "1.0.0-TEST", "VickyE2");
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
      NoiseSampler sampler = NoiseSampler.CELLULAR;
      sampler.setParameter("return", "CellValue");
      sampler.setParameter("salt", Utilities.generateRandomNumber());
      sampler.setParameter("frequency", "1 / 200 / ${customization.yml:cave-biome-scale} / ${customization.yml:global-scale}");
      extrusion.addBiome(new TestExtrusionBiome(), 4);
      extrusion.setSampler(sampler);
      this.addExtrusion(extrusion);
   }
}
