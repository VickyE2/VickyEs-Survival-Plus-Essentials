package org.vicky.vspe.systems.dimension.dimensions.ChromaticUnderWater;

import org.vicky.utilities.Version;
import org.vicky.vspe.systems.dimension.dimensions.ChromaticUnderWater.Biomes.*;
import org.vicky.vspe.systems.dimension.dimensions.ChromaticUnderWater.Extrusions.MossCaves;
import org.vicky.vspe.systems.dimension.Generator.BaseGenerator;
import org.vicky.vspe.systems.dimension.Generator.utils.Biome.extend.Tags;
import org.vicky.vspe.systems.dimension.Generator.utils.Extrusion.ReplaceExtrusion;
import org.vicky.vspe.systems.dimension.Generator.utils.Meta.BaseClass;
import org.vicky.vspe.systems.dimension.Generator.utils.Meta.Configuration;
import org.vicky.vspe.systems.dimension.Generator.utils.Meta.MetaClass;
import org.vicky.vspe.systems.dimension.Generator.utils.NoiseSampler.NoiseSampler;
import org.vicky.vspe.systems.dimension.Generator.utils.NoiseSampler.Samplers.CELLULAR;
import org.vicky.vspe.systems.dimension.Generator.utils.Range;
import org.vicky.vspe.systems.dimension.Generator.utils.Utilities;

public class ChromaticUnderwater extends BaseGenerator {
    public ChromaticUnderwater() {
        super("CHROMATIC_UNDERWATER", Version.parse("1.0.1-BIOME-TEST-ONE"), "VickyE2, UltraEazzi");

        this.addBiome(
                new CoralReefs() //new MysticalCoralReef(), //Coral Reefs
                //new SeagrassMeadows(), new BlueSeagrassMeadows(),
                //new DeadSeagrassMeadows() Seagrass Meadows
        );

        MetaClass meta = new MetaClass();
        meta.setOceanLevel(319);
        meta.setTemperatureVariance(0.2);
        meta.setHeightVariance(600.0);
        meta.setContinentalScale(1200);
        meta.setGlobalScale(1.5);
        BaseClass base = new BaseClass();
        base.setOceanLevel(319);
        Configuration conf = new Configuration.Builder()
                .terrainOceanBaseYLevel(319)
                .continentalScale(1200)
                .globalScale(1.5)
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
