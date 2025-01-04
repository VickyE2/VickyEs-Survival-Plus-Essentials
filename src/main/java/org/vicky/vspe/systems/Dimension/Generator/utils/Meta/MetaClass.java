package org.vicky.vspe.systems.Dimension.Generator.utils.Meta;

import org.vicky.vspe.systems.Dimension.Generator.utils.Range;

public class MetaClass {
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
        this.caveBiomeScale = 200;
        this.riverSpreadScale = 10;
        this.volcanoSpread = 150;
        this.volcanoRadius = 5;
        this.mushroomIslandSpread = 110;
        this.mushroomIslandRadius = 4;
        this.globalScale = 1;
        this.yLevel = 319;
        this.oceanLevel = 62;
        this.calculationMethod = HeightTemperatureRarityCalculationMethod.SEPARATE;
        this.strataDeepslate = new Range(7, -7);
        this.strataBedrock = new Range(-60, -64);
    }

    public void setStrataBedrock(Range strataBedrock) {
        this.strataBedrock = strataBedrock;
    }

    public void setTemperatureVariance(double temperatureVariance) {
        this.temperatureVariance = temperatureVariance;
    }

    public void setHeightVariance(double heightVariance) {
        this.heightVariance = heightVariance;
    }

    public void changeCalculationMethod(HeightTemperatureRarityCalculationMethod calculationMethod) {
        this.calculationMethod = calculationMethod;
    }

    public void setStrataDeepslate(Range strataDeepslate) {
        this.strataDeepslate = strataDeepslate;
    }

    public void setYLevel(int yLevel) {
        this.yLevel = yLevel;
    }

    public void setOceanLevel(int oceanLevel) {
        this.oceanLevel = oceanLevel;
    }

    public void setMushroomIslandRadius(int mushroomIslandRadius) {
        this.mushroomIslandRadius = mushroomIslandRadius;
    }

    public void setCaveBiomeScale(int caveBiomeScale) {
        this.caveBiomeScale = caveBiomeScale;
    }

    public void setContinentalScale(int continentalScale) {
        this.continentalScale = continentalScale;
    }

    public void setElevationScale(int elevationScale) {
        this.elevationScale = elevationScale;
    }

    public void setGlobalScale(double globalScale) {
        this.globalScale = globalScale;
    }

    public void setMushroomIslandSpread(int mushroomIslandSpread) {
        this.mushroomIslandSpread = mushroomIslandSpread;
    }

    public void setPrecipitationScale(int precipitationScale) {
        this.precipitationScale = precipitationScale;
    }

    public void setRiverSpreadScale(int riverSpreadScale) {
        this.riverSpreadScale = riverSpreadScale;
    }

    public void setTemperatureScale(int temperatureScale) {
        this.temperatureScale = temperatureScale;
    }

    public void setVariationScale(int variationScale) {
        this.variationScale = variationScale;
    }

    public void setVolcanoRadius(int volcanoRadius) {
        this.volcanoRadius = volcanoRadius;
    }

    public void setVolcanoSpread(int volcanoSpread) {
        this.volcanoSpread = volcanoSpread;
    }


}

