package org.vicky.vspe.systems.dimension.Generator.utils.Biome.type;

public class Precipitation {
    private final float precipitationAmount;
    private final PrecipitaionType precipitationType;

    public Precipitation(float precipitationAmount, PrecipitaionType precipitaionType) {
        this.precipitationAmount = precipitationAmount;
        this.precipitationType = precipitaionType;
    }

    public float getPrecipitationAmount() {
        return this.precipitationAmount;
    }

    public PrecipitaionType getPrecipitationType() {
        return this.precipitationType;
    }

    public enum PrecipitaionType {
        RAIN,
        SNOW
    }
}
