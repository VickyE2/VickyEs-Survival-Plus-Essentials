package org.vicky.vspe.systems.Dimension.Dimensions.Test.Features;

import org.vicky.vspe.systems.Dimension.Dimensions.Structures.TestFeatureStructure;
import org.vicky.vspe.systems.Dimension.Generator.utils.Feature.Feature;
import org.vicky.vspe.systems.Dimension.Generator.utils.Locator.Locators.RandomLocator;
import org.vicky.vspe.systems.Dimension.Generator.utils.Locator.Locators.Sampler;
import org.vicky.vspe.systems.Dimension.Generator.utils.Range;
import org.vicky.vspe.systems.Dimension.Generator.utils.NoiseSampler;
import org.vicky.vspe.systems.Dimension.Generator.utils.Ymlable;

public class TestFeature extends Feature {
    public TestFeature() {
        super("Test_Feature");
        addStructure(new TestFeatureStructure(), 10);


        NoiseSampler sampler = NoiseSampler.CONSTANT;
        sampler.setParameter("value", 1);
        Ymlable locatorSampler = new Sampler(sampler, 1.0f);
        setDistributor(locatorSampler);

        setLocator(new RandomLocator(new Range(2, 5), new Range(2, 5), 5552));

        setStructuresDistributor(sampler);
    }

}
