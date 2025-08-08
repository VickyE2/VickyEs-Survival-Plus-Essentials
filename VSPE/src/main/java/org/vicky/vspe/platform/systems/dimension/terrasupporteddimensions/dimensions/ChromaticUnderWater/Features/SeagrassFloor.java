package org.vicky.vspe.platform.systems.dimension.terrasupporteddimensions.dimensions.ChromaticUnderWater.Features;

import org.vicky.vspe.platform.systems.dimension.terrasupporteddimensions.dimensions.ChromaticUnderWater.Structures.SeagrassBed;
import org.vicky.vspe.platform.systems.dimension.terrasupporteddimensions.Generator.utils.Feature.Feature;
import org.vicky.vspe.platform.systems.dimension.terrasupporteddimensions.Generator.utils.Feature.utils.FeatureType;
import org.vicky.vspe.platform.systems.dimension.terrasupporteddimensions.Generator.utils.Locator.Locators.Context;
import org.vicky.vspe.platform.systems.dimension.terrasupporteddimensions.Generator.utils.Locator.Locators.OrLocator;
import org.vicky.vspe.platform.systems.dimension.terrasupporteddimensions.Generator.utils.Locator.Locators.PatternLocator;
import org.vicky.vspe.platform.systems.dimension.terrasupporteddimensions.Generator.utils.Locator.Locators.Patterns.Pattern;
import org.vicky.vspe.platform.systems.dimension.terrasupporteddimensions.Generator.utils.Locator.Locators.Sampler;
import org.vicky.vspe.platform.systems.dimension.terrasupporteddimensions.Generator.utils.NoiseSampler.NoiseSampler;
import org.vicky.vspe.platform.systems.dimension.terrasupporteddimensions.Generator.utils.NoiseSampler.Samplers.CONSTANT;
import org.vicky.vspe.platform.systems.dimension.terrasupporteddimensions.Generator.utils.Range;
import org.vicky.vspe.platform.systems.dimension.terrasupporteddimensions.Generator.utils.Utilities;
import org.vicky.vspe.platform.systems.dimension.terrasupporteddimensions.Generator.utils.Ymlable;
import org.vicky.vspe.platform.systems.dimension.vspeChunkGenerator.SimpleBlockState;

public class SeagrassFloor extends Feature {
    public SeagrassFloor() {
        super("SEAGRASS_FLOOR", FeatureType.FLORAL);
        addStructure(new SeagrassBed(), 10);

        CONSTANT CLR = new CONSTANT();
        NoiseSampler sampler = new CONSTANT().setParameter("salt", Utilities.generateRandomNumber());
        Ymlable distributor2 = new Sampler(sampler, 0.17F);
        Pattern pattern = new Pattern(Pattern.Type.AND);
        pattern.addSubPattern(new Pattern(Pattern.Type.MATCH).addBlock(SimpleBlockState.Companion.from("minecraft:water", (it) -> it).getMaterial(), 1));
        pattern.addSubPattern(new Pattern(Pattern.Type.MATCH).addBlock(SimpleBlockState.Companion.from("minecraft:moss_block", (it) -> it).getMaterial(), -1));
        Pattern pattern2 = new Pattern(Pattern.Type.AND);
        pattern2.addSubPattern(new Pattern(Pattern.Type.MATCH).addBlock(SimpleBlockState.Companion.from("minecraft:sand", (it) -> it).getMaterial(), -1));
        pattern2.addSubPattern(new Pattern(Pattern.Type.MATCH).addBlock(SimpleBlockState.Companion.from("minecraft:water", (it) -> it).getMaterial(), 1));
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
