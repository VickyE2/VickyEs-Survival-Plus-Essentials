package org.vicky.vspe.platform.systems.dimension.terrasupporteddimensions.dimensions.ChromaticUnderWater.Features;

import org.vicky.vspe.platform.systems.dimension.terrasupporteddimensions.Generator.utils.Locator.Locators.*;
import org.vicky.vspe.platform.systems.dimension.terrasupporteddimensions.dimensions.ChromaticUnderWater.Structures.HugeCoralGenerator;
import org.vicky.vspe.platform.systems.dimension.terrasupporteddimensions.Generator.utils.Feature.Feature;
import org.vicky.vspe.platform.systems.dimension.terrasupporteddimensions.Generator.utils.Feature.utils.FeatureType;
import org.vicky.vspe.platform.systems.dimension.terrasupporteddimensions.Generator.utils.Locator.Locators.*;
import org.vicky.vspe.platform.systems.dimension.terrasupporteddimensions.Generator.utils.Locator.Locators.Patterns.Pattern;
import org.vicky.vspe.platform.systems.dimension.terrasupporteddimensions.Generator.utils.NoiseSampler.Samplers.CELLULAR;
import org.vicky.vspe.platform.systems.dimension.terrasupporteddimensions.Generator.utils.NoiseSampler.Samplers.OPEN_SIMPLEX_2;
import org.vicky.vspe.platform.systems.dimension.terrasupporteddimensions.Generator.utils.NoiseSampler.Samplers.PROBABILITY;
import org.vicky.vspe.platform.systems.dimension.terrasupporteddimensions.Generator.utils.Range;
import org.vicky.vspe.platform.systems.dimension.terrasupporteddimensions.Generator.utils.Ymlable;
import org.vicky.vspe.platform.systems.dimension.vspeChunkGenerator.SimpleBlockState;

import static org.vicky.vspe.platform.systems.dimension.terrasupporteddimensions.Generator.utils.Utilities.generateRandomNumber;

public class LargeCorals extends Feature {
    public LargeCorals() {
        super("LARGE_CORALS", FeatureType.STRUCTURES);
        addStructure(new HugeCoralGenerator(), 1);

        CELLULAR CLR = new CELLULAR().setParameter("frequency", 0.02).setParameter("return", "CellValue");

        PaddedGrid grid = new PaddedGrid();
        grid.setWidth(65);
        grid.setPadding(4);
        grid.setSalt(generateRandomNumber());
        PROBABILITY distributor = new PROBABILITY()
                .setParameter("sampler", new OPEN_SIMPLEX_2()
                        .setParameter("frequency", 0.08)
                        .setParameter("salt", generateRandomNumber())
                );

        Ymlable distributorMl = new Sampler(distributor, 0.125f);

        AndLocator andLocator = new AndLocator(Context.DISTRIBUTOR);
        andLocator.addLocator(grid);
        andLocator.addLocator(distributorMl);


        Pattern pattern = new Pattern(Pattern.Type.AND);
        pattern.addSubPattern(new Pattern(Pattern.Type.MATCH).addBlock(SimpleBlockState.Companion.from("minecraft:water", (it) -> it).getMaterial(), 1));
        pattern.addSubPattern(new Pattern(Pattern.Type.MATCH).addBlock(SimpleBlockState.Companion.from("minecraft:moss_block", (it) -> it).getMaterial(), -1));

        AndLocator locator = new AndLocator(Context.LOCATOR);
        locator.addLocator(
                new TopLocator(new Range(45, 215))
        );
        locator.addLocator(
                new PatternLocator(
                        new Pattern(Pattern.Type.MATCH_SET)
                                .addBlock(SimpleBlockState.Companion.from("minecraft:grass_block", (it) -> it).getMaterial(), 0)
                                .addBlock(SimpleBlockState.Companion.from("minecraft:sand", (it) -> it).getMaterial(), 0)
                                .setOffset(-1),
                        new Range(45, 215)
                )
        );

        this.setDistributor(andLocator);
        this.setLocator(locator);
        this.setStructuresDistributor(CLR);
    }
}
