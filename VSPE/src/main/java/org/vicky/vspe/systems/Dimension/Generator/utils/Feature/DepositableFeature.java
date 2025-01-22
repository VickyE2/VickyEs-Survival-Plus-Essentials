package org.vicky.vspe.systems.Dimension.Generator.utils.Feature;

import java.util.ArrayList;
import java.util.List;
import org.vicky.vspe.systems.Dimension.Generator.utils.NoiseSampler;
import org.vicky.vspe.systems.Dimension.Generator.utils.Range;
import org.vicky.vspe.systems.Dimension.Generator.utils.Rarity;
import org.vicky.vspe.systems.Dimension.Generator.utils.Utilities;
import org.vicky.vspe.systems.Dimension.Generator.utils.Feature.utils.FeatureType;
import org.vicky.vspe.systems.Dimension.Generator.utils.Locator.Locators.GaussianRandomLocator;
import org.vicky.vspe.systems.Dimension.Generator.utils.Locator.Locators.RandomLocator;
import org.vicky.vspe.systems.Dimension.Generator.utils.Structures.DepositableStructure;

public class DepositableFeature extends Feature {
   public final List<DepositableStructure> anchorStructures;
   public final List<String> anchorStrings;
   public final int salt = Utilities.generateRandomNumber();
   public final Range yLevelRange;
   public final Rarity rarity;
   public final Type type;

   public DepositableFeature(String id, Rarity rarity, Range yLevelRange, OreDistributionType distributionType, Type type) {
      super(id, FeatureType.ABSTRACT);
      this.yLevelRange = yLevelRange;
      this.rarity = rarity;
      this.anchorStructures = new ArrayList<>();
      this.anchorStrings = new ArrayList<>();
      this.type = type;
      this.anchorStrings.add("");
      this.setDistributor(NoiseSampler.CONSTANT);
      this.structures.put("*structures", null);
      if (distributionType.equals(OreDistributionType.GAUSSIAN_RANDOM)) {
         GaussianRandomLocator oreDistributionType = new GaussianRandomLocator();
         oreDistributionType.setAmountRange(new Range(1, 1));
         oreDistributionType.setStandardDeviation("*standard-deviation");
         oreDistributionType.setHeightRange("*range");
         this.setDistributor(oreDistributionType);
      } else if (distributionType.equals(OreDistributionType.RANDOM)) {
         RandomLocator oreDistributionType = new RandomLocator();
         oreDistributionType.setAmountRange(new Range(1, 1));
         oreDistributionType.setHeightRange("*range");
         oreDistributionType.setSalt("*salt");
         this.setDistributor(oreDistributionType);
      }
   }

   public void addDepositable(DepositableStructure structure) {
      this.anchorStructures.add(structure);
   }

   public static enum Type {
      ORE,
      DEPOSIT;
   }
}
