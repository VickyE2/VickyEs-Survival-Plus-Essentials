package org.vicky.vspe.systems.Dimension.Dimensions.Test.Features;

import org.bukkit.Material;
import org.vicky.vspe.systems.Dimension.Generator.utils.Feature.Feature;
import org.vicky.vspe.systems.Dimension.Generator.utils.Locator.Locators.RandomLocator;
import org.vicky.vspe.systems.Dimension.Generator.utils.Locator.Locators.Sampler;
import org.vicky.vspe.systems.Dimension.Generator.utils.Range;
import org.vicky.vspe.systems.Dimension.Generator.utils.Structures.BaseStructure;
import org.vicky.vspe.systems.Dimension.Generator.utils.Structures.NoiseSampler.NoiseSampler;
import org.vicky.vspe.systems.Dimension.Generator.utils.Ymlable;
import org.vicky.vspe.utilities.Math.Vector3;

import java.util.Map;

public class TestFeature extends Feature {
    public TestFeature() {
        super("Test_Feature");
        BaseStructure baseStructure = new BaseStructure("Test_Structure");
        baseStructure.addCircle(10, new Vector3(0, 0, 0), new BaseStructure.MaterialPalette(BaseStructure.Direction.N, Map.of(Material.GOLD_BLOCK.getKey().asString(), 1)), true);
        addStructure(baseStructure, 10);

        NoiseSampler sampler = NoiseSampler.CONSTANT;
        sampler.setParameter("value", 1);
        Ymlable locatorSampler = new Sampler(sampler, 1.0f);
        setDistributor(locatorSampler);

        setLocator(new RandomLocator(new Range(2, 5), new Range(2, 5), 5552));

        setStructuresDistributor(sampler);
    }
}
