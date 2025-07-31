package org.vicky.vspe.systems.dimension.dimensions.ChromaticUnderWater.Features;

import org.bukkit.Material;
import org.vicky.vspe.systems.dimension.dimensions.ChromaticUnderWater.Structures.HugeCoralGenerator;
import org.vicky.vspe.systems.dimension.Generator.utils.Feature.Feature;
import org.vicky.vspe.systems.dimension.Generator.utils.Feature.utils.FeatureType;
import org.vicky.vspe.systems.dimension.Generator.utils.Locator.Locators.*;
import org.vicky.vspe.systems.dimension.Generator.utils.Locator.Locators.Patterns.Pattern;
import org.vicky.vspe.systems.dimension.Generator.utils.NoiseSampler.Samplers.CELLULAR;
import org.vicky.vspe.systems.dimension.Generator.utils.NoiseSampler.Samplers.OPEN_SIMPLEX_2;
import org.vicky.vspe.systems.dimension.Generator.utils.NoiseSampler.Samplers.PROBABILITY;
import org.vicky.vspe.systems.dimension.Generator.utils.Range;
import org.vicky.vspe.systems.dimension.Generator.utils.Ymlable;

import static org.vicky.vspe.systems.dimension.Generator.utils.Utilities.generateRandomNumber;

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
        pattern.addSubPattern(new Pattern(Pattern.Type.MATCH).addBlock(Material.WATER, 1));
        pattern.addSubPattern(new Pattern(Pattern.Type.MATCH).addBlock(Material.MOSS_BLOCK, -1));

        AndLocator locator = new AndLocator(Context.LOCATOR);
        locator.addLocator(
                new TopLocator(new Range(45, 215))
        );
        locator.addLocator(
                new PatternLocator(
                        new Pattern(Pattern.Type.MATCH_SET)
                                .addBlock(Material.GRASS_BLOCK, 0)
                                .addBlock(Material.SAND, 0)
                                .setOffset(-1),
                        new Range(45, 215)
                )
        );

        this.setDistributor(andLocator);
        this.setLocator(locator);
        this.setStructuresDistributor(CLR);
    }
}
