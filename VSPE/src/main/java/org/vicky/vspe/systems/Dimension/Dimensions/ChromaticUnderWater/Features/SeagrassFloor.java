package org.vicky.vspe.systems.Dimension.Dimensions.ChromaticUnderWater.Features;

import org.bukkit.Material;
import org.vicky.vspe.systems.Dimension.Dimensions.ChromaticUnderWater.Structures.SeagrassBed;
import org.vicky.vspe.systems.Dimension.Generator.utils.Feature.Feature;
import org.vicky.vspe.systems.Dimension.Generator.utils.Feature.utils.FeatureType;
import org.vicky.vspe.systems.Dimension.Generator.utils.Locator.Locators.Context;
import org.vicky.vspe.systems.Dimension.Generator.utils.Locator.Locators.OrLocator;
import org.vicky.vspe.systems.Dimension.Generator.utils.Locator.Locators.PatternLocator;
import org.vicky.vspe.systems.Dimension.Generator.utils.Locator.Locators.Patterns.Pattern;
import org.vicky.vspe.systems.Dimension.Generator.utils.Locator.Locators.Sampler;
import org.vicky.vspe.systems.Dimension.Generator.utils.NoiseSampler.NoiseSampler;
import org.vicky.vspe.systems.Dimension.Generator.utils.NoiseSampler.Samplers.CONSTANT;
import org.vicky.vspe.systems.Dimension.Generator.utils.Range;
import org.vicky.vspe.systems.Dimension.Generator.utils.Utilities;
import org.vicky.vspe.systems.Dimension.Generator.utils.Ymlable;

public class SeagrassFloor extends Feature {
    public SeagrassFloor() {
        super("SEAGRASS_FLOOR", FeatureType.FLORAL);
        addStructure(new SeagrassBed(), 10);

        CONSTANT CLR = new CONSTANT();
        NoiseSampler sampler = new CONSTANT().setParameter("salt", Utilities.generateRandomNumber());
        Ymlable distributor2 = new Sampler(sampler, 0.17F);
        Pattern pattern = new Pattern(Pattern.Type.AND, Pattern.Type.MATCH);
        pattern.addBlock(Material.WATER, 1);
        pattern.addBlock(Material.MOSS_BLOCK, -1);
        Pattern pattern2 = new Pattern(Pattern.Type.AND, Pattern.Type.MATCH);
        pattern2.addBlock(Material.WATER, 1);
        pattern2.addBlock(Material.SAND, -1);
        OrLocator and = new OrLocator(Context.LOCATOR);
        Ymlable locator = new PatternLocator(pattern, new Range(85, 102));
        Ymlable locator2 = new PatternLocator(pattern2, new Range(85, 319));
        and.addLocator(locator);
        and.addLocator(locator2);

        this.setDistributor(distributor2);
        this.setLocator(and);
        this.setStructuresDistributor(CLR);
    }
}
