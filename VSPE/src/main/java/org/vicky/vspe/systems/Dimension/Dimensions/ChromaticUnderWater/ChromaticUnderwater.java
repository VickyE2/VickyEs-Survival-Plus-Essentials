package org.vicky.vspe.systems.Dimension.Dimensions.ChromaticUnderWater;

import org.vicky.vspe.systems.Dimension.Dimensions.ChromaticUnderWater.Biomes.CoralReefs;
import org.vicky.vspe.systems.Dimension.Dimensions.ChromaticUnderWater.Biomes.MysticalCoralReef;
import org.vicky.vspe.systems.Dimension.Dimensions.Test.Biomes.TestExtrusionBiome;
import org.vicky.vspe.systems.Dimension.Generator.BaseGenerator;
import org.vicky.vspe.systems.Dimension.Generator.utils.NoiseSampler;
import org.vicky.vspe.systems.Dimension.Generator.utils.Range;
import org.vicky.vspe.systems.Dimension.Generator.utils.Utilities;
import org.vicky.vspe.systems.Dimension.Generator.utils.Biome.extend.Tags;
import org.vicky.vspe.systems.Dimension.Generator.utils.Extrusion.ReplaceExtrusion;
import org.vicky.vspe.systems.Dimension.Generator.utils.Meta.BaseClass;
import org.vicky.vspe.systems.Dimension.Generator.utils.Meta.MetaClass;

public class ChromaticUnderwater extends BaseGenerator {
   public ChromaticUnderwater() {
      super("CHROMATIC_UNDERWATER", "1.0.0-ALPHA", "VickyE2, UltraEazzi");
      this.addBiome(new CoralReefs());
      this.addBiome(new MysticalCoralReef());
      MetaClass meta = new MetaClass();
      meta.setOceanLevel(319);
      meta.setContinentalScale(256);
      meta.setTemperatureVariance(0.3);
      meta.setHeightVariance(600.0);
      BaseClass base = new BaseClass();
      base.setOceanLevel(319);
      this.setMeta(meta);
      this.setBase(base);
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
