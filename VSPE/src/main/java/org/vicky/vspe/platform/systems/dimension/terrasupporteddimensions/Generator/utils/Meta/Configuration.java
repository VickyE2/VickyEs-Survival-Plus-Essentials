package org.vicky.vspe.platform.systems.dimension.terrasupporteddimensions.Generator.utils.Meta;

import org.vicky.vspe.platform.systems.dimension.terrasupporteddimensions.Generator.utils.Ymlable;

public class Configuration implements Ymlable {
    // Parameters
    private int cellDistance = 350;
    private double cellJitter = 0.9;
    private double riverScale = 0.0013;
    private double edgeThreshold = 0.8;
    private double stretchFactor = 2.5;
    private double angle = 0.435;
    private double size = 0.83;
    private double borderSize = 0.02;
    private double frequency = 0.0025;
    private double mountainFrequency = 0.004;
    private int minY = 25;
    private double canyonAmount = 0.93;
    private double globalScale = 1;
    private int terrainBaseYLevel = 65;
    private int terrainHeight = 100;
    private int terrainOceanBaseYLevel = 60;
    private int terrainOceanDepth = 100;
    private int spawnIslandOriginX = 0;
    private int spawnIslandOriginZ = 0;
    private int spawnIslandRadiusInner = 20;
    private int spawnIslandRadiusMiddle = 125;
    private int spawnIslandRadiusOuter = 400;
    private double spawnIslandElevationScale = 0.3;
    private double continentalScale = 0.8;
    private double continentalOffset = 0.2;
    private int continentalCoastWidth = 30;
    private double continentalCoastThreshold = 0.05;
    private double elevationScale = 1;
    private double elevationHighlandsThreshold = 0.4;
    private double elevationFlatlandsThreshold = 0.1;
    private double temperatureScale = 0.4;
    private double temperatureOffset = 0;
    private double temperatureSpread = 1;
    private double temperatureAltitudeLapseRate = 1.3;
    private double temperatureAltitudeLapseStart = 0.2;
    private double precipitationScale = 1;
    private double precipitationOffset = 0;
    private double precipitationSpread = 1;
    private double precipitationOceanThreshold = -1;
    private double precipitationLandThreshold = 0.4;
    private double variationScale = 1;
    private double caveBiomeScale = 1;
    private double oceanDeepThreshold = -0.8;
    private double riverSpreadScale = 1;
    private int riverMaxWidth = 20;
    private int spotSpread = 2000;
    private int spotRadiusMin = 50;
    private int spotRadiusMax = 300;

    @Override
    public StringBuilder getYml() {
        StringBuilder yml = new StringBuilder();

        yml.append("biomeSpread:\n")
                .append("  cellDistance: ").append(cellDistance).append("\n")
                .append("  cellJitter: ").append(cellJitter).append("\n")
                .append("  riverScale: ").append(riverScale).append("\n")
                .append("  borderBiomes:\n")
                .append("    edgeThreshold: ").append(edgeThreshold).append("\n")
                .append("  sinkHoles:\n")
                .append("    stretchFactor: ").append(stretchFactor).append("\n")
                .append("    angle: ").append(angle).append("\n")
                .append("    size: ").append(size).append("\n")
                .append("    borderSize: ").append(borderSize).append("\n")
                .append("    frequency: ").append(frequency).append("\n")
                .append("  mountainFrequency: ").append(mountainFrequency).append("\n")
                .append("  canyons:\n")
                .append("    minY: ").append(minY).append("\n")
                .append("    canyonAmount: ").append(canyonAmount).append("\n")
                .append("global-scale: ").append(globalScale).append("\n")
                .append("terrain-base-y-level: ").append(terrainBaseYLevel).append("\n")
                .append("terrain-height: ").append(terrainHeight).append("\n")
                .append("terrain-ocean-base-y-level: ").append(terrainOceanBaseYLevel).append("\n")
                .append("terrain-ocean-depth: ").append(terrainOceanDepth).append("\n")
                .append("spawn-island-origin-x: ").append(spawnIslandOriginX).append("\n")
                .append("spawn-island-origin-z: ").append(spawnIslandOriginZ).append("\n")
                .append("spawn-island-radius-inner: ").append(spawnIslandRadiusInner).append("\n")
                .append("spawn-island-radius-middle: ").append(spawnIslandRadiusMiddle).append("\n")
                .append("spawn-island-radius-outer: ").append(spawnIslandRadiusOuter).append("\n")
                .append("spawn-island-elevation-scale: ").append(spawnIslandElevationScale).append("\n")
                .append("continental-scale: ").append(continentalScale).append("\n")
                .append("continental-offset: ").append(continentalOffset).append("\n")
                .append("continental-coast-width: ").append(continentalCoastWidth).append("\n")
                .append("continental-coast-threshold: ").append(continentalCoastThreshold).append("\n")
                .append("elevation-scale: ").append(elevationScale).append("\n")
                .append("elevation-highlands-threshold: ").append(elevationHighlandsThreshold).append("\n")
                .append("elevation-flatlands-threshold: ").append(elevationFlatlandsThreshold).append("\n")
                .append("temperature-scale: ").append(temperatureScale).append("\n")
                .append("temperature-offset: ").append(temperatureOffset).append("\n")
                .append("temperature-spread: ").append(temperatureSpread).append("\n")
                .append("temperature-altitude-lapse-rate: ").append(temperatureAltitudeLapseRate).append("\n")
                .append("temperature-altitude-lapse-start: ").append(temperatureAltitudeLapseStart).append("\n")
                .append("precipitation-scale: ").append(precipitationScale).append("\n")
                .append("precipitation-offset: ").append(precipitationOffset).append("\n")
                .append("precipitation-spread: ").append(precipitationSpread).append("\n")
                .append("precipitation-ocean-threshold: ").append(precipitationOceanThreshold).append("\n")
                .append("precipitation-land-threshold: ").append(precipitationLandThreshold).append("\n")
                .append("variation-scale: ").append(variationScale).append("\n")
                .append("cave-biome-scale: ").append(caveBiomeScale).append("\n")
                .append("ocean-deep-threshold: ").append(oceanDeepThreshold).append("\n")
                .append("river-spread-scale: ").append(riverSpreadScale).append("\n")
                .append("river-max-width: ").append(riverMaxWidth).append("\n")
                .append("spot-spread: ").append(spotSpread).append("\n")
                .append("spot-radius-min: ").append(spotRadiusMin).append("\n")
                .append("spot-radius-max: ").append(spotRadiusMax).append("\n");

        return yml;
    }

    // Builder class
    public static class Builder {
        private final Configuration config = new Configuration();

        public Builder cellDistance(int cellDistance) {
            config.cellDistance = cellDistance;
            return this;
        }

        public Builder cellJitter(double cellJitter) {
            config.cellJitter = cellJitter;
            return this;
        }

        public Builder riverScale(double riverScale) {
            config.riverScale = riverScale;
            return this;
        }

        public Builder edgeThreshold(double edgeThreshold) {
            config.edgeThreshold = edgeThreshold;
            return this;
        }

        public Builder stretchFactor(double stretchFactor) {
            config.stretchFactor = stretchFactor;
            return this;
        }

        public Builder angle(double angle) {
            config.angle = angle;
            return this;
        }

        public Builder size(double size) {
            config.size = size;
            return this;
        }

        public Builder borderSize(double borderSize) {
            config.borderSize = borderSize;
            return this;
        }

        public Builder frequency(double frequency) {
            config.frequency = frequency;
            return this;
        }

        public Builder mountainFrequency(double mountainFrequency) {
            config.mountainFrequency = mountainFrequency;
            return this;
        }

        public Builder minY(int minY) {
            config.minY = minY;
            return this;
        }

        public Builder canyonAmount(double canyonAmount) {
            config.canyonAmount = canyonAmount;
            return this;
        }

        public Builder globalScale(double globalScale) {
            config.globalScale = globalScale;
            return this;
        }

        public Builder terrainBaseYLevel(int terrainBaseYLevel) {
            config.terrainBaseYLevel = terrainBaseYLevel;
            return this;
        }

        public Builder terrainHeight(int terrainHeight) {
            config.terrainHeight = terrainHeight;
            return this;
        }

        public Builder terrainOceanBaseYLevel(int terrainOceanBaseYLevel) {
            config.terrainOceanBaseYLevel = terrainOceanBaseYLevel;
            return this;
        }

        public Builder terrainOceanDepth(int terrainOceanDepth) {
            config.terrainOceanDepth = terrainOceanDepth;
            return this;
        }

        public Builder spawnIslandOriginX(int spawnIslandOriginX) {
            config.spawnIslandOriginX = spawnIslandOriginX;
            return this;
        }

        public Builder spawnIslandOriginZ(int spawnIslandOriginZ) {
            config.spawnIslandOriginZ = spawnIslandOriginZ;
            return this;
        }

        public Builder spawnIslandRadiusInner(int spawnIslandRadiusInner) {
            config.spawnIslandRadiusInner = spawnIslandRadiusInner;
            return this;
        }

        public Builder spawnIslandRadiusMiddle(int spawnIslandRadiusMiddle) {
            config.spawnIslandRadiusMiddle = spawnIslandRadiusMiddle;
            return this;
        }

        public Builder spawnIslandRadiusOuter(int spawnIslandRadiusOuter) {
            config.spawnIslandRadiusOuter = spawnIslandRadiusOuter;
            return this;
        }

        public Builder spawnIslandElevationScale(double spawnIslandElevationScale) {
            config.spawnIslandElevationScale = spawnIslandElevationScale;
            return this;
        }

        public Builder continentalScale(double continentalScale) {
            config.continentalScale = continentalScale;
            return this;
        }

        public Builder continentalOffset(double continentalOffset) {
            config.continentalOffset = continentalOffset;
            return this;
        }

        public Builder continentalCoastWidth(int continentalCoastWidth) {
            config.continentalCoastWidth = continentalCoastWidth;
            return this;
        }

        public Builder continentalCoastThreshold(double continentalCoastThreshold) {
            config.continentalCoastThreshold = continentalCoastThreshold;
            return this;
        }

        public Builder elevationScale(double elevationScale) {
            config.elevationScale = elevationScale;
            return this;
        }

        public Builder elevationHighlandsThreshold(double elevationHighlandsThreshold) {
            config.elevationHighlandsThreshold = elevationHighlandsThreshold;
            return this;
        }

        public Builder elevationFlatlandsThreshold(double elevationFlatlandsThreshold) {
            config.elevationFlatlandsThreshold = elevationFlatlandsThreshold;
            return this;
        }

        public Builder temperatureScale(double temperatureScale) {
            config.temperatureScale = temperatureScale;
            return this;
        }

        public Builder temperatureOffset(double temperatureOffset) {
            config.temperatureOffset = temperatureOffset;
            return this;
        }

        public Builder temperatureSpread(double temperatureSpread) {
            config.temperatureSpread = temperatureSpread;
            return this;
        }

        public Builder temperatureAltitudeLapseRate(double temperatureAltitudeLapseRate) {
            config.temperatureAltitudeLapseRate = temperatureAltitudeLapseRate;
            return this;
        }

        public Builder temperatureAltitudeLapseStart(double temperatureAltitudeLapseStart) {
            config.temperatureAltitudeLapseStart = temperatureAltitudeLapseStart;
            return this;
        }

        public Builder precipitationScale(double precipitationScale) {
            config.precipitationScale = precipitationScale;
            return this;
        }

        public Builder precipitationOffset(double precipitationOffset) {
            config.precipitationOffset = precipitationOffset;
            return this;
        }

        public Builder precipitationSpread(double precipitationSpread) {
            config.precipitationSpread = precipitationSpread;
            return this;
        }

        public Builder precipitationOceanThreshold(double precipitationOceanThreshold) {
            config.precipitationOceanThreshold = precipitationOceanThreshold;
            return this;
        }

        public Builder precipitationLandThreshold(double precipitationLandThreshold) {
            config.precipitationLandThreshold = precipitationLandThreshold;
            return this;
        }

        public Builder variationScale(double variationScale) {
            config.variationScale = variationScale;
            return this;
        }

        public Builder caveBiomeScale(double caveBiomeScale) {
            config.caveBiomeScale = caveBiomeScale;
            return this;
        }

        public Builder oceanDeepThreshold(double oceanDeepThreshold) {
            config.oceanDeepThreshold = oceanDeepThreshold;
            return this;
        }

        public Builder riverSpreadScale(double riverSpreadScale) {
            config.riverSpreadScale = riverSpreadScale;
            return this;
        }

        public Builder riverMaxWidth(int riverMaxWidth) {
            config.riverMaxWidth = riverMaxWidth;
            return this;
        }

        public Builder spotSpread(int spotSpread) {
            config.spotSpread = spotSpread;
            return this;
        }

        public Builder spotRadiusMin(int spotRadiusMin) {
            config.spotRadiusMin = spotRadiusMin;
            return this;
        }

        public Builder spotRadiusMax(int spotRadiusMax) {
            config.spotRadiusMax = spotRadiusMax;
            return this;
        }

        public Configuration build() {
            return config;
        }
    }
}
