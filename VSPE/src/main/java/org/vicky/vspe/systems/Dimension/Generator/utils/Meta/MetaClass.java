package org.vicky.vspe.systems.Dimension.Generator.utils.Meta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.vicky.vspe.VSPE;
import org.vicky.vspe.systems.Dimension.Generator.utils.Range;
import org.vicky.vspe.systems.Dimension.Generator.utils.Utilities;
import org.vicky.vspe.systems.Dimension.Generator.utils.Ymlable;
import org.vicky.vspe.systems.Dimension.Generator.utils.Meta.misc.MetaMap;
import org.vicky.vspe.systems.Dimension.Generator.utils.Meta.misc.Tree;

public class MetaClass implements Ymlable {
   private final Map<String, Object> includedValues;
   private final Map<String, Object> customValues;
   private final List<Tree> customTrees;
   public int continentalScale = 100;
   public int yLevel;
   public int oceanLevel;
   public int temperatureScale = 60;
   public int precipitationScale;
   public int elevationScale;
   public int variationScale;
   public int caveBiomeScale;
   public int riverSpreadScale;
   public int volcanoSpread;
   public int volcanoRadius;
   public int mushroomIslandSpread;
   public int mushroomIslandRadius;
   public double globalScale;
   public Range strataDeepslate;
   public Range strataBedrock;
   public double temperatureVariance = 0.0;
   public double heightVariance = 1900.0;
   public HeightTemperatureRarityCalculationMethod calculationMethod;
   public HeightDistributionMethod heightDistributionMethod;
   public double oceanHeightVariance;

   public MetaClass() {
      this.precipitationScale = 50;
      this.elevationScale = 35;
      this.variationScale = 60;
      this.caveBiomeScale = 0;
      this.riverSpreadScale = 0;
      this.volcanoSpread = 0;
      this.volcanoRadius = 0;
      this.mushroomIslandSpread = 0;
      this.mushroomIslandRadius = 0;
      this.globalScale = 1.0;
      this.yLevel = 319;
      this.oceanLevel = 62;
      this.calculationMethod = HeightTemperatureRarityCalculationMethod.SEPARATE;
      this.heightDistributionMethod = HeightDistributionMethod.WELL_DISTRIBUTED;
      this.strataDeepslate = new Range(7, -7);
      this.strataBedrock = new Range(-60, -64);
      this.oceanHeightVariance = 0.0;
      this.includedValues = new HashMap<>();
      this.includedValues.put("continental-scale", this.continentalScale);
      this.includedValues.put("temperature-scale", this.temperatureScale);
      this.includedValues.put("temperature-variance", this.temperatureVariance);
      this.includedValues.put("height-variance", this.heightVariance);
      this.includedValues.put("precipitation-scale", this.precipitationScale);
      this.includedValues.put("elevation-scale", this.elevationScale);
      this.includedValues.put("variation-scale", this.variationScale);
      this.includedValues.put("global-scale", this.globalScale);
      this.includedValues.put("y-level", this.yLevel);
      this.includedValues.put("ocean-level", this.oceanLevel);
      this.customValues = new HashMap<>();
      this.customTrees = new ArrayList<>();
   }

   public void setOceanHeightVariance(double oceanHeightVariance) {
      this.oceanHeightVariance = oceanHeightVariance;
   }

   public void setStrataBedrock(Range strataBedrock) {
      this.strataBedrock = strataBedrock;
   }

   public void setTemperatureVariance(double temperatureVariance) {
      this.temperatureVariance = temperatureVariance;
      this.includedValues.put("temperature-variance", temperatureVariance);
   }

   public void setHeightVariance(double heightVariance) {
      this.heightVariance = heightVariance;
      this.includedValues.put("height-variance", heightVariance);
   }

   public void changeCalculationMethod(HeightTemperatureRarityCalculationMethod calculationMethod) {
      this.calculationMethod = calculationMethod;
   }

   public void setStrataDeepslate(Range strataDeepslate) {
      this.strataDeepslate = strataDeepslate;
   }

   public void setYLevel(int yLevel) {
      this.yLevel = yLevel;
      this.includedValues.put("y-level", yLevel);
   }

   public void setOceanLevel(int oceanLevel) {
      this.oceanLevel = oceanLevel;
      this.includedValues.put("ocean-level", oceanLevel);
   }

   public void setMushroomIslandRadius(int mushroomIslandRadius) {
      this.mushroomIslandRadius = mushroomIslandRadius;
      this.includedValues.put("mushroom-island-radius", mushroomIslandRadius);
   }

   public void setCaveBiomeScale(int caveBiomeScale) {
      this.caveBiomeScale = caveBiomeScale;
      this.includedValues.put("cave-biome-scale", this.mushroomIslandRadius);
   }

   public void setContinentalScale(int continentalScale) {
      this.continentalScale = continentalScale;
      this.includedValues.put("continental-scale", continentalScale);
   }

   public void setElevationScale(int elevationScale) {
      this.elevationScale = elevationScale;
      this.includedValues.put("elevation-scale", elevationScale);
   }

   public void setGlobalScale(double globalScale) {
      this.globalScale = globalScale;
      this.includedValues.put("global-scale", globalScale);
   }

   public void setMushroomIslandSpread(int mushroomIslandSpread) {
      this.mushroomIslandSpread = mushroomIslandSpread;
      this.includedValues.put("mushroom-island-spread", mushroomIslandSpread);
   }

   public void setPrecipitationScale(int precipitationScale) {
      this.precipitationScale = precipitationScale;
      this.includedValues.put("precipitation-scale", precipitationScale);
   }

   public void setRiverSpreadScale(int riverSpreadScale) {
      this.riverSpreadScale = riverSpreadScale;
      this.includedValues.put("river-spread-scale", riverSpreadScale);
   }

   public void setTemperatureScale(int temperatureScale) {
      this.temperatureScale = temperatureScale;
      this.includedValues.put("temperature-scale", temperatureScale);
   }

   public void setVariationScale(int variationScale) {
      this.variationScale = variationScale;
      this.includedValues.put("variation-scale", variationScale);
   }

   public void setVolcanoRadius(int volcanoRadius) {
      this.volcanoRadius = volcanoRadius;
      this.includedValues.put("volcano-radius", volcanoRadius);
   }

   public void setVolcanoSpread(int volcanoSpread) {
      this.volcanoSpread = volcanoSpread;
      this.includedValues.put("volcano-spread", volcanoSpread);
   }

   public void addBiomeDistributionValue(String key, Object value) {
      this.includedValues.put(key, value);
   }

   public void addValue(String key, Object value) {
      this.customValues.put(key, value);
   }

   public void addTree(Tree tree) {
      this.customTrees.add(tree);
   }

   public MetaMap getMappingFor(String map) {
      for (Tree tree : this.customTrees) {
         String mapped = tree.getMapping(map);
         if (mapped != null) {
            return new MetaMap("meta.yml:" + mapped);
         }
      }

      if (this.customValues.containsKey(map)) {
         return new MetaMap("meta.yml:" + map);
      } else {
         String closestMatch = null;
         int minDistance = Integer.MAX_VALUE;

         for (String key : this.includedValues.keySet()) {
            int distance = Utilities.getLevenshteinDistance(map, key);
            if (distance < minDistance) {
               minDistance = distance;
               closestMatch = key;
            }
         }

         for (String keyx : this.customValues.keySet()) {
            int distance = Utilities.getLevenshteinDistance(map, keyx);
            if (distance < minDistance) {
               minDistance = distance;
               closestMatch = keyx;
            }
         }

         if (closestMatch != null) {
            VSPE.getInstancedLogger().warning("Failed to get meta mapping for map: " + map + ". Did you mean: " + closestMatch + "?");
            return null;
         } else {
            VSPE.getInstancedLogger().warning("Failed to get meta mapping for map: " + map);
            return null;
         }
      }
   }

   public MetaMap getMappingFor(MetaVariables variable) {
      return new MetaMap(variable.getMapping());
   }

   @Override
   public StringBuilder getYml() {
      StringBuilder builder = new StringBuilder();
      builder.append("#========================[META-CLASS]========================").append("\n");
      builder.append("biome-distribution:").append("\n");

      for (Entry<String, Object> objectEntry : this.includedValues.entrySet()) {
         builder.append("  ").append(objectEntry.getKey()).append(": ").append(objectEntry.getValue()).append("\n");
      }

      if (!this.customValues.isEmpty()) {
         builder.append("#<=--------------------[CUSTOM-VALUES]---------------------=>").append("\n");

         for (Entry<String, Object> entry : this.customValues.entrySet()) {
            builder.append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
         }
      }

      if (!this.customTrees.isEmpty()) {
         builder.append("#<=--------------------[CUSTOM-NESTS]---------------------=>").append("\n");

         for (Tree tree : this.customTrees) {
            builder.append((CharSequence)tree.getYml()).append("\n");
         }
      }

      builder.append("strata:")
         .append("\n")
         .append("  deepslate:")
         .append("\n")
         .append("    top: ")
         .append(this.strataDeepslate.getMax())
         .append("\n")
         .append("    bottom: ")
         .append(this.strataDeepslate.getMin())
         .append("\n")
         .append("  bedrock:")
         .append("\n")
         .append("    top: ")
         .append(this.strataBedrock.getMax())
         .append("\n")
         .append("    bottom: ")
         .append(this.strataBedrock.getMin())
         .append("\n");
      builder.append(
         "palette-bottom:\n  - DEEPSLATE: $meta.yml:strata.deepslate.top\n  - BEDROCK: $meta.yml:strata.bedrock.top\n  - BLOCK:minecraft:bedrock: $meta.yml:strata.bedrock.bottom\n\npalette-bedrock:\n  - BEDROCK: $meta.yml:strata.bedrock.top\n  - BLOCK:minecraft:bedrock: $meta.yml:strata.bedrock.bottom\n"
      );
      return builder;
   }

   public static enum MetaVariables {
      PALETTE_BOTTOM_DEEPSLATE("strata.deepslate.bottom"),
      PALETTE_TOP_DEEPSLATE("strata.deepslate.top"),
      PALETTE_BOTTOM_BEDROCK("strata.bedrock.bottom"),
      PALETTE_TOP_BEDROCK("strata.bedrock.top"),
      HEIGHT_VARIANCE("biome-distribution.height-variance"),
      TEMPERATURE_VARIANCE("biome-distribution.temperature-variance"),
      GLOBAL_SCALE("biome-distribution.global-scale"),
      MUSHROOM_ISLAND_RADIUS("biome-distribution.mushroom-island-radius"),
      MUSHROOM_ISLAND_SPREAD("biome-distribution.mushroom-island-spread"),
      VOLCANO_RADIUS("biome-distribution.volcano-radius"),
      VOLCANO_SPREAD("biome-distribution.volcano-spread"),
      continentalScale("biome-distribution.continental-scale"),
      yLevel("biome-distribution.y-level"),
      oceanLevel("biome-distribution.ocean-level"),
      temperatureScale("biome-distribution.temperature-scale"),
      precipitationScale("biome-distribution.precipitation-scale"),
      elevationScale("biome-distribution.elevation-scale"),
      variationScale("biome-distribution.variation-scale"),
      caveBiomeScale("biome-distribution.cave-biome-scale"),
      riverSpreadScale("biome-distribution.river-spread-scale");

      private final String mapping;

      private MetaVariables(String mapping) {
         this.mapping = mapping;
      }

      private String getMapping() {
         return "meta.yml:" + this.mapping;
      }
   }
}
