package org.vicky.vspe.systems.Dimension.Generator.utils.Meta;

import org.vicky.vspe.VSPE;
import org.vicky.vspe.systems.Dimension.Generator.utils.Meta.misc.MetaMap;
import org.vicky.vspe.systems.Dimension.Generator.utils.Meta.misc.Tree;
import org.vicky.vspe.systems.Dimension.Generator.utils.Range;
import org.vicky.vspe.systems.Dimension.Generator.utils.Utilities;
import org.vicky.vspe.systems.Dimension.Generator.utils.Ymlable;

import java.util.*;

import static org.vicky.vspe.systems.Dimension.Generator.utils.Utilities.getLevenshteinDistance;

public class MetaClass implements Ymlable {
    private final Map<String, Object> includedValues;
    private final Map<String, Object> customValues;
    private final List<Tree> customTrees;
    public int continentalScale;
    public int yLevel;
    public int oceanLevel;
    public int temperatureScale;
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
    public double temperatureVariance;
    public double heightVariance;
    public HeightTemperatureRarityCalculationMethod calculationMethod;


    public MetaClass() {
        this.continentalScale = 100;
        this.temperatureScale = 60;
        this.temperatureVariance = 0;
        this.heightVariance = 1900;
        this.precipitationScale = 50;
        this.elevationScale = 35;
        this.variationScale = 60;
        this.caveBiomeScale = 0;
        this.riverSpreadScale = 0;
        this.volcanoSpread = 0;
        this.volcanoRadius = 0;
        this.mushroomIslandSpread = 0;
        this.mushroomIslandRadius = 0;
        this.globalScale = 1;
        this.yLevel = 319;
        this.oceanLevel = 62;
        this.calculationMethod = HeightTemperatureRarityCalculationMethod.SEPARATE;
        this.strataDeepslate = new Range(7, -7);
        this.strataBedrock = new Range(-60, -64);

        includedValues = new HashMap<>();
        includedValues.put("continental-scale", continentalScale);
        includedValues.put("temperature-scale", temperatureScale);
        includedValues.put("temperature-variance", temperatureVariance);
        includedValues.put("height-variance", heightVariance);
        includedValues.put("precipitation-scale", precipitationScale);
        includedValues.put("elevation-scale", elevationScale);
        includedValues.put("variation-scale", variationScale);
        includedValues.put("global-scale", globalScale);
        includedValues.put("y-level", yLevel);
        includedValues.put("ocean-level", oceanLevel);

        customValues = new HashMap<>();
        customTrees = new ArrayList<>();
    }

    public void setStrataBedrock(Range strataBedrock) {
        this.strataBedrock = strataBedrock;
    }

    public void setTemperatureVariance(double temperatureVariance) {
        this.temperatureVariance = temperatureVariance;
        includedValues.put("temperature-variance", temperatureVariance);
    }

    public void setHeightVariance(double heightVariance) {
        this.heightVariance = heightVariance;
        includedValues.put("height-variance", heightVariance);
    }

    public void changeCalculationMethod(HeightTemperatureRarityCalculationMethod calculationMethod) {
        this.calculationMethod = calculationMethod;
    }

    public void setStrataDeepslate(Range strataDeepslate) {
        this.strataDeepslate = strataDeepslate;
    }

    public void setYLevel(int yLevel) {
        this.yLevel = yLevel;
        includedValues.put("y-level", yLevel);
    }

    public void setOceanLevel(int oceanLevel) {
        this.oceanLevel = oceanLevel;
        includedValues.put("ocean-level", oceanLevel);
    }

    public void setMushroomIslandRadius(int mushroomIslandRadius) {
        this.mushroomIslandRadius = mushroomIslandRadius;
        includedValues.put("mushroom-island-radius", mushroomIslandRadius);
    }

    public void setCaveBiomeScale(int caveBiomeScale) {
        this.caveBiomeScale = caveBiomeScale;
        includedValues.put("cave-biome-scale", mushroomIslandRadius);
    }

    public void setContinentalScale(int continentalScale) {
        this.continentalScale = continentalScale;
        includedValues.put("continental-scale", continentalScale);
    }

    public void setElevationScale(int elevationScale) {
        this.elevationScale = elevationScale;
        includedValues.put("elevation-scale", elevationScale);
    }

    public void setGlobalScale(double globalScale) {
        this.globalScale = globalScale;
        includedValues.put("global-scale", globalScale);
    }

    public void setMushroomIslandSpread(int mushroomIslandSpread) {
        this.mushroomIslandSpread = mushroomIslandSpread;
        includedValues.put("mushroom-island-spread", mushroomIslandSpread);
    }

    public void setPrecipitationScale(int precipitationScale) {
        this.precipitationScale = precipitationScale;
        includedValues.put("precipitation-scale", precipitationScale);
    }

    public void setRiverSpreadScale(int riverSpreadScale) {
        this.riverSpreadScale = riverSpreadScale;
        includedValues.put("river-spread-scale", riverSpreadScale);
    }

    public void setTemperatureScale(int temperatureScale) {
        this.temperatureScale = temperatureScale;
        includedValues.put("temperature-scale", temperatureScale);
    }

    public void setVariationScale(int variationScale) {
        this.variationScale = variationScale;
        includedValues.put("variation-scale", variationScale);
    }

    public void setVolcanoRadius(int volcanoRadius) {
        this.volcanoRadius = volcanoRadius;
        includedValues.put("volcano-radius", volcanoRadius);
    }

    public void setVolcanoSpread(int volcanoSpread) {
        this.volcanoSpread = volcanoSpread;
        includedValues.put("volcano-spread", volcanoSpread);
    }

    public void addBiomeDistributionValue(String key, Object value) {
        includedValues.put(key, value);
    }

    public void addValue(String key, Object value) {
        customValues.put(key, value);
    }

    public void addTree(Tree tree) {
        customTrees.add(tree);
    }

    public MetaMap getMappingFor(String map) {
        for (Tree tree : customTrees) {
            String mapped = tree.getMapping(map);
            if (mapped != null) {
                return new MetaMap("meta.yml:" + mapped);
            }
        }

        if (customValues.containsKey(map)) {
            return new MetaMap("meta.yml:" + map);
        }

        String closestMatch = null;
        int minDistance = Integer.MAX_VALUE;

        for (String key : includedValues.keySet()) {
            int distance = getLevenshteinDistance(map, key);
            if (distance < minDistance) {
                minDistance = distance;
                closestMatch = key;
            }
        }

        for (String key : customValues.keySet()) {
            int distance = getLevenshteinDistance(map, key);
            if (distance < minDistance) {
                minDistance = distance;
                closestMatch = key;
            }
        }

        if (closestMatch != null) {
            VSPE.getInstancedLogger().warning("Failed to get meta mapping for map: " + map + ". Did you mean: " + closestMatch + "?");
            return null;
        }

        VSPE.getInstancedLogger().warning("Failed to get meta mapping for map: " + map);
        return null;
    }


    public MetaMap getMappingFor(MetaVariables variable) {
        return new MetaMap(variable.getMapping());
    }

    @Override
    public StringBuilder getYml() {
        StringBuilder builder = new StringBuilder();

        builder.append("#========================[META-CLASS]========================").append("\n");
        builder.append("biome-distribution:").append("\n");
        for (Map.Entry<String, Object> objectEntry : includedValues.entrySet()) {
            builder.append("  ").append(objectEntry.getKey()).append(": ").append(objectEntry.getValue()).append("\n");
        }
        if (!customValues.isEmpty()) {
            builder.append("#<=--------------------[CUSTOM-VALUES]---------------------=>").append("\n");
            for (Map.Entry<String, Object> entry : customValues.entrySet()) {
                builder.append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
            }
        }
        if (!customTrees.isEmpty()) {
            builder.append("#<=--------------------[CUSTOM-NESTS]---------------------=>").append("\n");
            for (Tree tree : customTrees)
                builder.append(tree.getYml()).append("\n");
        }
        builder.append("strata:").append("\n")
                .append("  deepslate:").append("\n")
                .append("    top: ").append(strataDeepslate.getMax()).append("\n")
                .append("    bottom: ").append(strataDeepslate.getMin()).append("\n")
                .append("  bedrock:").append("\n")
                .append("    top: ").append(strataBedrock.getMax()).append("\n")
                .append("    bottom: ").append(strataBedrock.getMin()).append("\n");

        builder.append("""
                palette-bottom:
                  - DEEPSLATE: $meta.yml:strata.deepslate.top
                  - BEDROCK: $meta.yml:strata.bedrock.top
                  - BLOCK:minecraft:bedrock: $meta.yml:strata.bedrock.bottom
                                
                palette-bedrock:
                  - BEDROCK: $meta.yml:strata.bedrock.top
                  - BLOCK:minecraft:bedrock: $meta.yml:strata.bedrock.bottom
                """);

        return builder;
    }


    public enum MetaVariables {
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
        MetaVariables (String mapping) {
            this.mapping = mapping;
        }

        private String getMapping() {
            return "meta.yml:" + mapping;
        }
    }
}

