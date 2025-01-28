package org.vicky.vspe.systems.Dimension.Dimensions.ChromaticUnderWater;

import org.vicky.vspe.systems.Dimension.Dimensions.ChromaticUnderWater.Biomes.CoralReefs;
import org.vicky.vspe.systems.Dimension.Dimensions.ChromaticUnderWater.Biomes.MysticalCoralReef;
import org.vicky.vspe.systems.Dimension.Dimensions.ChromaticUnderWater.Extrusions.MossCaves;
import org.vicky.vspe.systems.Dimension.Dimensions.Test.Biomes.TestExtrusionBiome;
import org.vicky.vspe.systems.Dimension.Generator.BaseGenerator;
import org.vicky.vspe.systems.Dimension.Generator.utils.Biome.extend.Tags;
import org.vicky.vspe.systems.Dimension.Generator.utils.Extrusion.ReplaceExtrusion;
import org.vicky.vspe.systems.Dimension.Generator.utils.Meta.BaseClass;
import org.vicky.vspe.systems.Dimension.Generator.utils.Meta.Configuration;
import org.vicky.vspe.systems.Dimension.Generator.utils.Meta.MetaClass;
import org.vicky.vspe.systems.Dimension.Generator.utils.NoiseSampler.NoiseSampler;
import org.vicky.vspe.systems.Dimension.Generator.utils.NoiseSampler.Samplers.CELLULAR;
import org.vicky.vspe.systems.Dimension.Generator.utils.Range;
import org.vicky.vspe.systems.Dimension.Generator.utils.Utilities;

public class ChromaticUnderwater extends BaseGenerator {
    public ChromaticUnderwater() {
        super("CHROMATIC_UNDERWATER", "1.0.0-ALPHA", "VickyE2, UltraEazzi");

        this.addBiome(new CoralReefs(), new MysticalCoralReef());

        MetaClass meta = new MetaClass();
        meta.setOceanLevel(319);
        meta.setContinentalScale(256);
        meta.setTemperatureVariance(0.3);
        meta.setHeightVariance(600.0);
        BaseClass base = new BaseClass();
        base.setOceanLevel(319);
        Configuration conf = new Configuration.Builder()
                .terrainOceanBaseYLevel(319)
                .build();
        this.setMeta(meta);
        this.setBase(base);
        this.setConfiguration(conf);
        ReplaceExtrusion extrusion = new ReplaceExtrusion("add_underground_biome", 2);
        extrusion.setFrom(Tags.ALL);
        extrusion.setRange(new Range(-20, 10));
        NoiseSampler sampler = new CELLULAR();
        sampler.setVoidParameter("return", "CellValue");
        sampler.setVoidParameter("salt", Utilities.generateRandomNumber());
        sampler.setVoidParameter("frequency", "1 / 200 / ${customization.yml:cave-biome-scale} / ${customization.yml:global-scale}");
        extrusion.addBiome(new MossCaves(), 4);
        extrusion.setSampler(sampler);
        this.addExtrusion(extrusion);
    }
}
