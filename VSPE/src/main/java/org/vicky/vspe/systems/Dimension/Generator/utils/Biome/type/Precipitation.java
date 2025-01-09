package org.vicky.vspe.systems.Dimension.Generator.utils.Biome.type;

public class Precipitation {

    public enum PrecipitaionType {
        RAIN, SNOW
    }

    private final float precipitationAmount;
    private final PrecipitaionType precipitationType;


    public Precipitation(float precipitationAmount, PrecipitaionType precipitaionType) {
        this.precipitationAmount = precipitationAmount;
        this.precipitationType = precipitaionType;
    }

    public float getPrecipitationAmount() {
        return precipitationAmount;
    }

    public PrecipitaionType getPrecipitationType() {
        return precipitationType;
    }
}
