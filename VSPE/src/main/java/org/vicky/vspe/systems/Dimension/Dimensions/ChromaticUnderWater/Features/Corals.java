package org.vicky.vspe.systems.Dimension.Dimensions.ChromaticUnderWater.Features;

import org.bukkit.Material;
import org.vicky.vspe.systems.Dimension.Dimensions.ChromaticUnderWater.Structures.HugeCoralTree;
import org.vicky.vspe.systems.Dimension.Generator.utils.Feature.Feature;
import org.vicky.vspe.systems.Dimension.Generator.utils.Feature.utils.FeatureType;
import org.vicky.vspe.systems.Dimension.Generator.utils.Locator.Locators.*;
import org.vicky.vspe.systems.Dimension.Generator.utils.Locator.Locators.Patterns.Pattern;
import org.vicky.vspe.systems.Dimension.Generator.utils.NoiseSampler.Samplers.CELLULAR;
import org.vicky.vspe.systems.Dimension.Generator.utils.NoiseSampler.Samplers.OPEN_SIMPLEX_2;
import org.vicky.vspe.systems.Dimension.Generator.utils.NoiseSampler.Samplers.PROBABILITY;
import org.vicky.vspe.systems.Dimension.Generator.utils.Range;
import org.vicky.vspe.systems.Dimension.Generator.utils.Ymlable;

import static org.vicky.vspe.systems.Dimension.Generator.utils.Utilities.generateRandomNumber;

public class Corals extends Feature {
    public Corals() {
        super("CORALS", FeatureType.STRUCTURES);
        addStructure(new HugeCoralTree(), 10);

        CELLULAR CLR = new CELLULAR().setParameter("frequency", 0.08).setParameter("return", "CellValue");

        AndLocator andLocator = new AndLocator(Context.DISTRIBUTOR);
        PaddedGrid grid = new PaddedGrid();
        grid.setWidth(12);
        grid.setPadding(1);
        grid.setSalt(generateRandomNumber());
        PROBABILITY distributor = new PROBABILITY()
                .setParameter("sampler", new OPEN_SIMPLEX_2()
                        .setParameter("frequency", 0.08)
                        .setParameter("salt", generateRandomNumber())
                );
        Ymlable distributorMl = new Sampler(distributor, 0.125f);
        andLocator.addLocator(grid);
        andLocator.addLocator(distributorMl);

        Pattern pattern = new Pattern(Pattern.Type.AND, Pattern.Type.MATCH);
        pattern.addBlock(Material.WATER, 1);
        pattern.addBlock(Material.MOSS_BLOCK, -1);
        Ymlable locator = new PatternLocator(pattern, new Range(90, 319));

        /*
        AndLocator andLocator = new AndLocator(Context.DISTRIBUTOR);
        PaddedGrid grid = new PaddedGrid();
            grid.setWidth(45);
            grid.setPadding(5);
            grid.setSalt(generateRandomNumber());
        PROBABILITY distributor = new PROBABILITY()
                .setParameter("sampler", new OPEN_SIMPLEX_2()
                        .setParameter("frequency", 0.08)
                        .setParameter("salt", generateRandomNumber())
                );
        Ymlable distributorMl = new Sampler(distributor, 0.125f);
        andLocator.addLocator(grid);
        andLocator.addLocator(distributorMl);

        CONSTANT sampler = new CONSTANT();
        */

        this.setDistributor(andLocator);

        this.setLocator(locator);
        this.setStructuresDistributor(CLR);
    }
}
