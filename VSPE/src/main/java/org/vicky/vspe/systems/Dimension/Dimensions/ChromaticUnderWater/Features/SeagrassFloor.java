package org.vicky.vspe.systems.Dimension.Dimensions.ChromaticUnderWater.Features;

import org.vicky.vspe.systems.Dimension.Dimensions.ChromaticUnderWater.Structures.SeagrassBed;
import org.vicky.vspe.systems.Dimension.Generator.utils.Feature.Feature;
import org.vicky.vspe.systems.Dimension.Generator.utils.Feature.utils.FeatureType;
import org.vicky.vspe.systems.Dimension.Generator.utils.Locator.Locators.RandomLocator;
import org.vicky.vspe.systems.Dimension.Generator.utils.Locator.Locators.Sampler;
import org.vicky.vspe.systems.Dimension.Generator.utils.NoiseSampler.NoiseSampler;
import org.vicky.vspe.systems.Dimension.Generator.utils.NoiseSampler.Samplers.CONSTANT;
import org.vicky.vspe.systems.Dimension.Generator.utils.Range;
import org.vicky.vspe.systems.Dimension.Generator.utils.Ymlable;

public class SeagrassFloor extends Feature {
    public SeagrassFloor() {
        super("SEAGRASS_FLOOR", FeatureType.FLORAL);
        addStructure(new SeagrassBed(), 10);

        CONSTANT sampler = new CONSTANT()
                .setParameter("value", 1);
        Ymlable distributor = new Sampler(sampler, 1.0f);
        this.setDistributor(distributor);
        this.setLocator(new RandomLocator(new Range(2, 5), new Range(2, 5), 5552));
        this.setStructuresDistributor(sampler);
    }
}
